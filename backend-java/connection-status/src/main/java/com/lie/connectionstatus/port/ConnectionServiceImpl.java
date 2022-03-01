package com.lie.connectionstatus.port;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lie.connectionstatus.domain.PingTimer;
import com.lie.connectionstatus.domain.RetryPingTask;
import com.lie.connectionstatus.domain.user.Authority;
import com.lie.connectionstatus.domain.user.User;
import com.lie.connectionstatus.domain.user.UserConnection;
import com.lie.connectionstatus.domain.user.UserConnectionManager;
import com.lie.connectionstatus.domain.room.Room;
import com.lie.connectionstatus.domain.room.RoomManager;
import com.lie.connectionstatus.dto.CreateEventDto;
import com.lie.connectionstatus.dto.JoinEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor @Service
public class ConnectionServiceImpl implements ConnectionService{
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final UserConnectionManager userConnectionManager;
    private final RoomManager roomManager;
    private final RoomRepository roomRepository;
    private final MessageInterface messageInterface;
    private final ObjectMapper objectMapper;

    @Override
    public void createRoom( WebSocketSession session, String username) throws Exception{
        //닉네임 랜덤 배정 시 checkUsername 지워도됨
        if(!roomManager.checkIfUsernameExists(username)){
            Room room = roomManager.createRoom();

            roomRepository.save(room);
            roomManager.createMediaPipeline(room);

            User newParticipant = new User(username,session.getId(), Authority.LEADER);

            //user에게 pipeline 주고, 시스템에 저장해주기
            room = roomManager.joinRoom(session, room, newParticipant);

            room = roomRepository.save(room);

            CreateEventDto createEventDto = new CreateEventDto("create", room);
            messageInterface.publishEventToKafka("create",objectMapper.writeValueAsString(createEventDto));
            return;
        }
        throw new Exception();
    }

    @Override
    public void joinRoom( WebSocketSession session, String username, String roomId) throws Exception{
        Room room = roomRepository.findById(roomId).orElseThrow();
        User newParticipant = new User(username, session.getId(), Authority.PLAYER);

        room = roomManager.joinRoom(session, room, newParticipant);

        room = roomRepository.save(room);
        JoinEventDto joinEventDto = new JoinEventDto("join", room.getRoomId(), newParticipant);
        messageInterface.publishEventToKafka("join", objectMapper.writeValueAsString(joinEventDto));
    }

    @Override
    public void leaveRoom(WebSocketSession session) throws Exception {
        if(userConnectionManager.checkIfUserDoesNotExists(session.getId())){
           log.info("USER doesn't exist. There is no one to leave");
           return;
        }

        UserConnection participant = userConnectionManager.getBySession(session.getId());
        Room room = roomRepository.findById(participant.getRoomId()).orElseThrow();

        if(room.checkIfLeader(participant.getUsername())){
            room = roomManager.leave(participant,room);
            roomManager.close( room);
            roomRepository.delete(room);
            return;
        }

        room = roomManager.leave(participant, room);

        roomRepository.save(room);
    }

    @Override
    public Room checkIfRoomExists(String roomId) {
        roomRepository.existsById(roomId);
        return roomRepository.findById(roomId).orElseThrow();
    }

    @Override
    public Boolean checkIfUsernameExistsInRoom(String roomId, String username) {
        Room room = roomRepository.findById(roomId).orElseThrow();

        return room.checkIfUserExists(username);
    }

    @Scheduled(fixedDelay=40000)
    private void sendPingMessageToClients(){
        PingMessage pingMessage = new PingMessage();
        userConnectionManager.getUsersBySessionId().values().stream().forEach(session -> {
            try{
                session.getSession().sendMessage(pingMessage);
                log.info(userConnectionManager.getUsersBySessionId().size()+" "+"healthy clients left");
                return;
            } catch (IOException e) {
                log.info("PING MESSAGE OUT ERROR");
            } catch (Exception e) {
                log.info("UNEXPECTED ERROR");

                PingTimer pingTimer = new PingTimer();
                RetryPingTask retryPing = new RetryPingTask(userConnectionManager,  objectMapper, this);
                retryPing.setClientSession(session.getSession());
                pingTimer.schedule(retryPing, 40000);
            }
        });
    }
}
