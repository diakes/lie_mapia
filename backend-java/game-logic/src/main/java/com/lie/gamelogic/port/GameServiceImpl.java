package com.lie.gamelogic.port;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lie.gamelogic.domain.*;
import com.lie.gamelogic.dto.*;
import com.lie.gamelogic.dto.Start.*;
import com.lie.gamelogic.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//service 롤백의 개념 transcation 처리
@Service
@Slf4j
@RequiredArgsConstructor
public class GameServiceImpl implements GameService{

    private final MessageInterface messageInterface;
    private final RoomRepository roomRepository;
    private final VoteRepository voteRepository;
    private final ExecutionVoteRepository executionVoteRepository;
    private final ObjectMapper objectMapper;

    //Timer 지정 
    ConcurrentHashMap<String, Timer> gameTimerByRoomId = new ConcurrentHashMap<>();
    //Session을 지정
    ConcurrentHashMap<String, WebSocketSession> gameSessionByRoomId = new ConcurrentHashMap<>();

    @Override
    public void createGameRoom(Room room) {
        roomRepository.save(room);
    }

    @Override
    public void joinGameRoom(JoinGameRoomDto joinGameRoomDto) {
        log.info(joinGameRoomDto.toString());
        Room room = roomRepository.findById(joinGameRoomDto.getRoomId()).orElseThrow();
        room = room.join(joinGameRoomDto.getUser());
        roomRepository.save(room);

    }

    @Override
    public void leaveGameRoom(String username, String roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow();
        room = room.leave(username);
        roomRepository.save(room);

    }

    @Override
    public void closeGameRoom(String roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow();
        log.info(room.toString());
        if(room.getRoomPhase().equals(RoomPhase.MORNINGVOTE) || room.getRoomPhase().equals((RoomPhase.NIGHTVOTE))){
            deleteVote(roomId);
        }
        if(room.getRoomPhase().equals(RoomPhase.EXECUTIONVOTE)){
            deleteExecutionVote(roomId);
        }
        gameTimerByRoomId.get(roomId).cancel();
        
        room.close();
        log.info(room.toString());
        roomRepository.deleteById(roomId);

    }

    @Override
    public void pressStart(String sessionId, String roomId, String username) throws IOException {
        Room room = roomRepository.findById(roomId).orElseThrow();

        //test용으로
        room.setRoomStatus(RoomStatus.WAITING);

        //시작 상태면 예외 처리
        if(room.getRoomStatus() == RoomStatus.START) {
            log.info("Error");
            //session.sendMessage(new TextMessage("Already Started!!!"));
            return;
        }
        room = room.pressStart(username);

        //방이 없으면 예외 처리
        if(ObjectUtils.isEmpty(room)){
            log.info("Error");
            //session.sendMessage(new TextMessage("Start failed"));
            return;
        }

        messageInterface.publishStartEvent("start", roomId);

        roomRepository.save(room);
        //message produce

        if(gameTimerByRoomId.get(roomId) == null)
            gameTimerByRoomId.put(roomId,new Timer());
        else {
            gameTimerByRoomId.remove(roomId,gameTimerByRoomId.get(roomId));
            gameTimerByRoomId.put(roomId,new Timer());
        }

        GameTurn gameTurn = new GameTurnImpl(roomRepository,this);
        gameTurn.setnextWork(roomId,gameTimerByRoomId.get(roomId));

    }

    @Override
    public void pressReady(String sessionId, String roomId, String username) {
        Room room = roomRepository.findById(roomId).orElseThrow();

        if(!room.checkIfUserExists(username)){
            log.info("User {} doesn't exist in Room {}",username, roomId);
            return;
        }
        if(room.checkIfUserIsLeader(username)){
            log.info("User {} is a leader");
            return;
        }
        room = room.pressReady(username);
        User user = room.getUserByUsername(username);
        //messageInterface.publishReadyEvent("ready", user, roomId);

        roomRepository.save(room);

        //메시지 전송에 대한 것
        ReadUserDto readUserDto = new ReadUserDto("game","ready",roomId,username,user.getReady());
        try {
            messageInterface.publishReadyEvent("client.response"
                    ,roomRepository.findById(roomId).orElseThrow()
                    ,objectMapper.writeValueAsString(readUserDto));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        //produce ready
    }

    @Override
    public void roleAssign(String roomId) {
        Room room=roomRepository.findById(roomId).orElseThrow();
        room=room.initStartGame(); //alive true, 직업배정
        room.setEndTime(TimeUtils.getFinTime(15));
        roomRepository.save(room);

        List<String> userList = new ArrayList<>();

        room.getParticipants().forEach((player,user)->{
            userList.add(user.getUsername());
        });

        LocalDateTime endTime = room.getEndTime();

        room.getParticipants().forEach((player,user)->{
           MadeDataDto roleDto =
                   new MadeDataDto("game","roleAssign",new roleAssignDto(roomId,
                     userList,endTime,user.getJob()));

           log.info(roleDto.toString());

           try {
               messageInterface.publishReponseEvent("client.response"
                       ,user
                       ,objectMapper.writeValueAsString(roleDto));
           } catch (JsonProcessingException e) {
               e.printStackTrace();
           }
       });
    }

//    @Override
//    public void createVote(String roomId, RoomPhase phase) {
//
//        switch (phase){
//            case EXECUTIONVOTE :
//                ExecutionVote executionVote=new ExecutionVote();
//                executionVoteRepository.save(executionVote.createVote(roomId,phase));
//                log.info(executionVote.toString());
//                break;
//            default:
//                Vote vote=new Vote();
//                voteRepository.save(vote.createVote(roomId,phase));
//                vote=vote.createVote(roomId,phase);
//                log.info(vote.toString());
//                break;
//        }
//    }
    @Override
    public void createMovingVote(String roomId){
        Vote vote=new Vote();
        voteRepository.save(vote.createVote(roomId,RoomPhase.MORNING));
        vote=vote.createVote(roomId,RoomPhase.MORNING);
        log.info(vote.toString());
    }

    @Override
    public void createExecutionVote(String roomId){
        ExecutionVote executionVote=new ExecutionVote();
        executionVoteRepository.save(executionVote.createVote(roomId,RoomPhase.EXECUTIONVOTE));
        log.info(executionVote.toString());
    }

    @Override
    public void createNightVote(String roomId){
        Vote vote=new Vote();
        voteRepository.save(vote.createVote(roomId,RoomPhase.NIGHTVOTE));
        vote=vote.createVote(roomId,RoomPhase.NIGHTVOTE);
        log.info(vote.toString());
    }
    @Override
    public void selectMoringVote(String sessionId,String roomId, String username, String select) {
        Room room = roomRepository.findById(roomId).orElseThrow();
        User user = room.getUserByUsername(username);
        if (!user.getAlive()) { //살아있는 user만 select
            log.info("User {} died in Room {}", username, roomId);
            return;
        }

        Vote vote = voteRepository.findById("vote" + roomId).orElseThrow();
        UserVote userVote = new UserVote(username, user.getSessionId(), user.getJob(), select);

        vote.putUserVote(username, userVote);
        voteRepository.save(vote);
        MadeDataDto voteDto = new MadeDataDto("game", "citizenVote",new ClientVoteDto("MorningVote",room.getRoomId(), username, select));
        try {
            messageInterface.publishSelectEvent("client.response"
                    ,room
                    ,objectMapper.writeValueAsString(voteDto));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void selectNightVote(String sessionId,String roomId, String username, String select) {
        Room room =roomRepository.findById(roomId).orElseThrow();
        User user=room.getUserByUsername(username);
        if(!user.getAlive()){ //살아있는 user만 select
            log.info("User {} died in Room {}", username, roomId);
            return;
        }

        if(user.getJob().equals(Job.CITIZEN)){
            log.info("User {} doesn't have to Vote");
            return;
        }

        Vote vote=voteRepository.findById("vote"+roomId).orElseThrow();
        UserVote userVote=new UserVote(username,user.getSessionId(),user.getJob(),select);

        vote.putUserVote(username,userVote);
        voteRepository.save(vote);
        MadeDataDto voteDto = new MadeDataDto("game", "citizenVote",new ClientVoteDto("NightVote",room.getRoomId(), username, select));

        List<User> coworker = new ArrayList<>();
        List<User> Doctor = new ArrayList<>();

        room.getParticipants().forEach((player,name)->{
            if(name.getJob().equals(Job.MAFIA)) {
                coworker.add(name);
            }
            else if(name.getJob().equals(Job.DOCTOR)) {
                Doctor.add(name);
            }
        });

        try {
            if(user.getJob().equals(Job.MAFIA)) {
                messageInterface.publishReponseEvent("client.response"
                        , coworker
                        , objectMapper.writeValueAsString(voteDto));
            }
            if(user.getJob().equals(Job.DOCTOR)){
                messageInterface.publishReponseEvent("client.response"
                        , Doctor
                        , objectMapper.writeValueAsString(voteDto));
            }

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        log.info(vote.toString());
    }

    @Override
    public void selectExecutionVote(String sessionId,String roomId, String username, String select, RoomPhase roomPhase, boolean agree) {
        Room room =roomRepository.findById(roomId).orElseThrow();

        User user=room.getUserByUsername(username);
        if(!user.getAlive()){ //살아있는 user만 select
            log.info("User {} died in Room {}", username, roomId);
            return;
        }

        if(username.equals(select)){ //선택받은 사용자가 투표시 return
            log.info("User {} select user in Room {}", username, roomId);
            return;
        }
        if(!room.getResult().equals(select)){ //의심자가 아닌 사용자를 선택한 경우 return
            log.info("User {} is not selected user in Room {}", username, roomId);
            return;
        }

        ExecutionVote vote=executionVoteRepository.findById("executionvote"+roomId).orElseThrow();

        UserExecutionVote userExecutionVote=vote.getVotes().get(username);
        if(userExecutionVote==null){
            userExecutionVote=new UserExecutionVote(username,user.getSessionId(),select,agree,false);
            vote.putUserVote(username,userExecutionVote);
        }

        vote=vote.pressVoted(username,agree);
        executionVoteRepository.save(vote);

        MadeDataDto executionVoteDto = new MadeDataDto("game","madeVote",new ClientExecutionVoteDto(
        "executionVote",roomId,username,select,agree,vote.getAgreeDie(), vote.getAgreeAlive()));

        //생존자 체크
        List<User> alive = new ArrayList<>();

        room.getParticipants().forEach((player,user1)-> {
            if(user1.getUsername().equals(select)){
                log.info("exception");
            }
            else if (user1.getAlive()) {
                alive.add(user1);
            }
        });

        try {
            messageInterface.publishReponseEvent("client.response"
                    ,alive
                    ,objectMapper.writeValueAsString(executionVoteDto));

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        log.info(vote.toString());
    }

    @Override
    public void resultMornigVote(String roomId) {
        Vote vote=voteRepository.findById("vote"+roomId).orElseThrow();
        List<String> list= new ArrayList(vote.selectList()); //투표 내용 가져오기
        Map<String,Integer> voteResult=new HashMap<>();
        log.info(list.toString());

        String username="";
        int max=0;
        for(String select:list){
            if(!voteResult.containsKey(select)){
                voteResult.put(select,1); //해당 user가 처음으로 투표 받았을때
                if(max==0) max=1;
            }else{
                voteResult.put(select,voteResult.get(select)+1);
            }

            if(voteResult.get(select)>max){ // 최다 득표자 갱신
                max=voteResult.get(select);
                username=select;
            } else if(voteResult.get(select)==max){ //동점자일 경우 null
                username=null;
            }
        };

        Room room=roomRepository.findById(roomId).orElseThrow();
        room.setResult(username);

        roomRepository.save(room);

        log.info(room.toString());
    }

    @Override
    public void deleteVote(String roomId) {
        Vote vote=voteRepository.findById("vote"+roomId).orElseThrow();
        voteRepository.delete(vote);
    }

    @Override
    public void resultExecutionVote(String roomId) {
        ExecutionVote vote=executionVoteRepository.findById("executionvote"+roomId).orElseThrow();
        Room room=roomRepository.findById(roomId).orElseThrow();
        if(vote.getAgreeDie()*2<=vote.getVotes().size()){
            room.setResult(null);
        }
        roomRepository.save(room);
    }

    @Override
    public void resultNightVote(String roomId) {
        Room room=roomRepository.findById(roomId).orElseThrow();
        Vote vote=voteRepository.findById("vote"+roomId).orElseThrow();

        HashMap<String,UserVote> voteMap=vote.getVotes();
        Set<String> mafiaSelect=new HashSet<>();
        String doctorSelect="";

        for (UserVote jobVote: voteMap.values()){
            if(jobVote.getJob().equals(Job.MAFIA)){
                mafiaSelect.add(jobVote.getSelect());
            }else if(jobVote.getJob().equals(Job.DOCTOR)){
                doctorSelect=jobVote.getSelect();
            }
        }

        if (mafiaSelect.size()==1){ //죽일사람
            for(String selectName:mafiaSelect){
                if(!selectName.equals(doctorSelect)){
                    room.setResult(selectName);
                    roomRepository.save(room);
                    log.info(room.getResult());
                    return;
                }
            }

        } //사망자는 마피아가 죽일사람을 선택했을때, 의사가 못살리면 세팅

        room.setResult(null); //그 외는 죽은 사람이 없다.
        log.info(room.getResult());
        roomRepository.save(room);

    }

    @Override
    public void deleteExecutionVote(String roomId) {
        ExecutionVote vote=executionVoteRepository.findById("executionvote"+roomId).orElseThrow();
        executionVoteRepository.delete(vote);
    }

    @Override
    public void dead(String roomId, String username) {
        //room 정보를 가져옴
        Room room = roomRepository.findById(roomId).orElseThrow();

        //null 일시
        if(username == null){
            log.info("no One Dead");
            return;
        }
        //방안에 없을시
        if(!room.checkIfUserExists(username)){
            log.info("User {} doesn't exist in Room {}",username, roomId);
            return;
        }
        User user = room.getParticipants().get(username);
        //이미 죽어 있을 시
        if(!user.getAlive()){
            log.info("User {} is already dead ", username);
            return;
        }
        user.setAlive(false);

        log.info("User {} is dead ", username);
        messageInterface.publishDeadEvent("dead",user,roomId);
        roomRepository.save(room);

        //gameEnd(roomId);
    }

    @Override
    public void gameEnd(String roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow();
        Integer citizenCount = 0;
        Integer mapiaCount =0;
        //시민 리스트
        List<String> citizenList = new ArrayList<>();
        //마피아 리스트
        List<String> mapiaList = new ArrayList<>();
        //이긴 직업
        Job Winner = null;
        //진 직업
        Job Loser = null;
        GameEndDto gameEndDto = null;
        Set set = room.getParticipants().keySet();
        Iterator iterator = set.iterator();

        while(iterator.hasNext()){
            User user = room.getParticipants().get(iterator.next());
            //살아 있는 경우에 사용 해줌
            Job job = user.getJob();
            if (job.equals(Job.CITIZEN)) {
                citizenList.add(user.getUsername());
                if(user.getAlive())
                    citizenCount++;
            }
            else if (job.equals(Job.DOCTOR)){
                citizenList.add(user.getUsername());
                if(user.getAlive())
                    citizenCount++;
            }
            else {
                mapiaList.add(user.getUsername());
                if(user.getAlive())
                    mapiaCount++;
            }
        }
        //마피아가 한명도 없을 때
        if(mapiaCount == 0) {
            //log.info("Citizen wins winner is : {} ", citizenList);
            Winner = Job.CITIZEN;
            Loser = Job.MAFIA;
            gameEndDto = new GameEndDto(
                    new ResultDto(roomRepository.findById(roomId).orElseThrow().getResult(),
                            new FindDto(Winner,citizenList),
                            new FindDto(Loser,mapiaList)));
        }
        else if(mapiaCount >= citizenCount){ // 마피아가 시민 수보다 많을 때
            //log.info("Mapia wins winner is : {} " , mapiaList);
            Winner = Job.MAFIA;
            Loser = Job.CITIZEN;
            gameEndDto = new GameEndDto(
                    new ResultDto(roomRepository.findById(roomId).orElseThrow().getResult(),
                            new FindDto(Winner,mapiaList),
                            new FindDto(Loser,citizenList)));
        }
        else{
            log.info("Game is not end");
            return;
        }

        //topic을 end로 만듦
        room.setGameResult(gameEndDto);
        //그리고 room 정보를 변경해주고 이를 저장해줌
        roomRepository.save(room);

    }

    @Override
    public void GameEndMessage(Room room, GameEndDto gameEndDto){
        try {
            messageInterface.publishReponseEvent("client.response",room,objectMapper.writeValueAsString(new MadeDataDto("game","end",gameEndDto)));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }


    public void StartMeesage(Room room){
        //생존자 넣는다.
        List<String> aliveList = new ArrayList<>();
        List<User> aliveUserList = new ArrayList<>();
        List<String> coworker = new ArrayList<>();
        room.getParticipants().forEach((player,user)->{
            if(user.getAlive()) {
                aliveList.add(user.getUsername());
                aliveUserList.add(user);
            }
            if(user.getJob().equals(Job.MAFIA)){
                coworker.add(user.getUsername());
            }
        });

        MadeDataDto DataDto = null;

        //현재 페이지 에 맞춰서 메시지를 넣어줌
        switch(room.getRoomPhase()){
            case MORNING:
                try {
                    DataDto = new MadeDataDto("game","startMorning",
                            new startMorningDto(room.getRoomId(),
                                    room.getEndTime(),room.getResult(),aliveList,room.getDay(),true));
                    messageInterface.publishReponseEvent("client.response",room,objectMapper.writeValueAsString(DataDto));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                break;
            case MORNINGVOTE:
                room.getParticipants().forEach((player,user)->{
                    MadeDataDto startMorngingVoteDto =
                             new MadeDataDto("game","startMorningVote"
                        ,new startMorngingVoteDto(room.getRoomId(),room.getEndTime(),aliveList,user.getAlive()));


                    log.info(startMorngingVoteDto.toString());

                    try {
                        messageInterface.publishReponseEvent("client.response"
                                ,user
                                ,objectMapper.writeValueAsString(startMorngingVoteDto));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                });

            break;
            case FINALSPEECH :
                try {
                    DataDto = new MadeDataDto("game","startFinalSpeech",
                                    new FinalSpeechDto(room.getRoomId(),
                                    room.getEndTime(),room.getResult()));
                    messageInterface.publishReponseEvent("client.response",room,objectMapper.writeValueAsString(DataDto));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                break;
            case EXECUTIONVOTE:
                MadeDataDto executionVoteDto = null;
                executionVoteDto = new MadeDataDto("game", "startExecutionVote",
                            new ExecutionVoteDto(
                             room.getRoomId(), room.getEndTime(), room.getResult()));


                try {
                            messageInterface.publishReponseEvent("client.response"
                                    ,aliveUserList
                                    ,objectMapper.writeValueAsString(executionVoteDto));
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                        break;
            case NIGHTVOTE:
                room.getParticipants().forEach((player,user)->{
                    Job job = null;
                    Boolean votable = false;
                    MadeDataDto nightVoteDto = null;
                   if(user.getJob().equals(Job.MAFIA)) {
                       job = Job.MAFIA;
                       votable = true;
                       coworker.add(user.getUsername());
                   }
                   else if(user.getJob().equals(Job.DOCTOR)) {
                       job = Job.DOCTOR;
                       votable = true;
                   }
                   else job = Job.CITIZEN;

                   if(job.equals(Job.MAFIA)) {

                       nightVoteDto =
                                   new MadeDataDto("game","startNightVote",
                                           new NightVoteDto(
                                           room.getRoomId(), room.getResult(),room.getEndTime(), aliveList, votable, coworker));
                   }
                   else{

                           nightVoteDto =
                                   new MadeDataDto("game","startNightVote",new NightVoteDto(
                                           room.getRoomId(), room.getResult(),room.getEndTime(), aliveList, votable, null));
                   }
                    try {
                        messageInterface.publishReponseEvent("client.response"
                                ,user
                                ,objectMapper.writeValueAsString(nightVoteDto));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                });break;
        }
    }
}
