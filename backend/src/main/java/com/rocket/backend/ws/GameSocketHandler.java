package com.rocket.backend.ws;

import com.rocket.backend.bet.CashedOutEvent;
import com.rocket.backend.game.CurrentRound;
import com.rocket.backend.game.RoundChangedEvent;
import com.rocket.backend.game.RoundEngine;
import com.rocket.backend.game.RoundView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameSocketHandler extends TextWebSocketHandler {

    private final RoundEngine roundEngine;
    private final ObjectMapper json;

    /** Everyone connected right now. Key = session id.  当前所有连线 */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // ---------- connection lifecycle  连线生命周期 ----------

    @Override
    public void afterConnectionEstablished(WebSocketSession raw) {
        // Wrap the session: thread-safe sending, and drop clients that are too slow
        // 包装:多线程安全发送;太慢的客户端(2 秒、64KB 积压)会被断开
        WebSocketSession s = new ConcurrentWebSocketSessionDecorator(raw, 2_000, 64 * 1024);
        sessions.put(raw.getId(), s);
        log.info("WS connected {} (total {})", raw.getId(), sessions.size());

        // Rule 2: tell the newcomer what's true RIGHT NOW  告诉新连线"现在"的真实状态
        CurrentRound c = roundEngine.current();
        if (c != null) {
            send(s, new RoundMessage("SNAPSHOT", RoundView.from(c)));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession raw, TextMessage message) {
        ClientMessage msg = json.readValue(message.getPayload(), ClientMessage.class);
        if ("PING".equals(msg.type())) {
            send(sessions.get(raw.getId()),
                    Map.of("type", "PONG", "serverTimeMs", System.currentTimeMillis()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession raw, CloseStatus status) {
        sessions.remove(raw.getId());
        log.info("WS closed {} ({}), total {}", raw.getId(), status.getCode(), sessions.size());
    }

    // ---------- pushing  推送 ----------

    /** RoundEngine published an event → push it to everyone.  收到回合事件 → 推送给所有人 */
    @EventListener
    public void onRoundChanged(RoundChangedEvent e) {
        broadcast(new RoundMessage(e.type(), RoundView.from(e.round())));
    }

    private void broadcast(Object payload) {
        TextMessage m = new TextMessage(json.writeValueAsString(payload));   // serialise ONCE  只转换一次
        sessions.values().forEach(s -> sendRaw(s, m));
    }

    private void send(WebSocketSession s, Object payload) {
        if (s != null) sendRaw(s, new TextMessage(json.writeValueAsString(payload)));
    }

    private void sendRaw(WebSocketSession s, TextMessage m) {
        try {
            if (s.isOpen()) s.sendMessage(m);
        } catch (Exception ex) {
            // One broken client must never stop the broadcast to everyone else
            // 一个坏掉的客户端,绝不能影响其他人收到消息
            log.debug("WS send failed {}: {}", s.getId(), ex.getMessage());
        }
    }

    @TransactionalEventListener   // org.springframework.transaction.event.TransactionalEventListener
    public void onCashedOut(CashedOutEvent e) {
        broadcast(Map.of("type", "CASHED_OUT", "username", e.username(), "multiplier", e.multiplier()));
    }
}