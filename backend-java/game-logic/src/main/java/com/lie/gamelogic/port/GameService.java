package com.lie.gamelogic.port;

import com.lie.gamelogic.domain.Room;
import com.lie.gamelogic.domain.RoomPhase;
import com.lie.gamelogic.domain.User;
import com.lie.gamelogic.dto.GameEndDto;
import com.lie.gamelogic.dto.JoinGameRoomDto;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Service
public interface GameService {

    // 메소드는 호출하는 순간 파악 할 수 있어야 한다.
    void createGameRoom(Room room);
    void joinGameRoom(JoinGameRoomDto joinGameRoomDto);
    void leaveGameRoom(String username, String roomId);
    void closeGameRoom(String roomId);
    void pressReady(String sessionId, String roomId, String username);
    void pressStart(String sessionId, String roomId, String username) throws IOException;
    void roleAssign(String roomId);
    void createMovingVote(String roomId);
    void createExecutionVote(String roomId);
    void createNightVote(String roomId);
    void selectMoringVote(String sessionId,String roomId, String username, String select);
    void selectNightVote(String sessionId,String roomId,String username,String select);
    void selectExecutionVote(String sessionId,String roomId,String username,String select,RoomPhase roomPhase,boolean agree);
    void resultMornigVote(String roomId);
    void resultExecutionVote(String roomId);
    void resultNightVote(String roomId);
    void deleteVote(String roomId);
    void deleteExecutionVote(String roomId);
    void dead(String roomId,String person);
    void gameEnd(String roomId);
    void GameEndMessage(Room room, GameEndDto gameEndDto);

}
