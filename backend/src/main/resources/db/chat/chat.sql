-- 채팅방 테이블
CREATE TABLE IF NOT EXISTS chat_room
(
    room_id           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '채팅방 PK',
    accommodations_id BIGINT   NOT NULL COMMENT '숙소 PK',
    reservation_id    BIGINT   NOT NULL COMMENT '예약 PK',
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    CONSTRAINT PK_CHAT_ROOM PRIMARY KEY (room_id),
    CONSTRAINT FK_CHAT_ROOM_ACC FOREIGN KEY (accommodations_id) REFERENCES accommodation (accommodations_id),
    CONSTRAINT FK_CHAT_ROOM_RSV FOREIGN KEY (reservation_id) REFERENCES reservation (reservation_id)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4;

-- 채팅 메시지 테이블
CREATE TABLE IF NOT EXISTS chat_message
(
    message_id BIGINT   NOT NULL AUTO_INCREMENT COMMENT '메시지 PK',
    room_id    BIGINT   NOT NULL COMMENT '채팅방 PK',
    user_id    BIGINT   NOT NULL COMMENT '보낸 회원 PK',
    content    TEXT     NOT NULL COMMENT '메시지 내용',
    sent_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '전송 시각',
    CONSTRAINT PK_CHAT_MESSAGE PRIMARY KEY (message_id),
    CONSTRAINT FK_CHAT_MESSAGE_ROOM FOREIGN KEY (room_id) REFERENCES chat_room (room_id),
    CONSTRAINT FK_CHAT_MESSAGE_USER FOREIGN KEY (user_id) REFERENCES users (user_id)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4;