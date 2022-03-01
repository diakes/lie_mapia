package com.lie.connectionstatus.domain.room;

import com.lie.connectionstatus.domain.user.Authority;
import com.lie.connectionstatus.domain.user.User;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;
import org.springframework.util.ObjectUtils;

import java.util.*;

@Slf4j
@Data
@RedisHash(value = "room")
@ApiModel(description = "connection 서버에서 방 정보를 저장하고 관리하는 모델입니다.")
public class Room {
    @Id
    @Indexed
    @ApiModelProperty(notes = "방 고유 id 입니다. Redis에 저장될 때 임의로 생성되며 숫자가 아닌 String 값입니다")
    private String roomId;

    @ApiModelProperty(notes = "방에 접속하고 있는 유저들의 리스트입니다. username을 키로 관리됩니다")
    private HashMap<String, User> participants = new HashMap<>();
    @ApiModelProperty(notes = "방의 상태를 나타냅니다. WAITING / STARTING 두 가지가 있습니다.")
    private RoomStatus roomStatus;

    public Room(){
        this.roomStatus = RoomStatus.WAITING;
    }
    public Boolean checkIfFull(){
        if(this.participants.size()>=8){
            log.info("The Room is Full");
            return true;
        }
        return false;
    }
    public Boolean checkIfUserExists(String username){
        if(ObjectUtils.isEmpty(participants.get(username))){
            return false;
        }
        return true;
    }
    public Room join(User participant){
        log.info("ROOM {}: participant {} entering", this.roomId, participant.getUsername());
        this.participants.put(participant.getUsername(), participant);

        return this;
    }
    public Boolean checkIfLeader(String username){
        if(participants.get(username).getAuthority().equals(Authority.LEADER)){
            return true;
        }
        return false;
    }
    public Boolean checkIfEmpty(){
        if(participants.isEmpty()){
            return true;
        }
        return false;
    }
    public void leave(String username){
        log.info("ROOM {}: participant {} leaving", this.roomId, username);
        this.participants.remove(username);
    }
    public void close(){
        log.info("Room {} : closing", this.getRoomId());
        this.participants.clear();
    }


}
