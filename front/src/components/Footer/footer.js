import React, { useState, useEffect, useRef } from "react";
import { useHistory } from "react-router-dom";
import { Button } from "react-bootstrap";
import styled from "styled-components";
import { BsFillCameraVideoOffFill } from "react-icons/bs";
import { BsFillCameraVideoFill } from "react-icons/bs";
import { BsFillMicMuteFill } from "react-icons/bs";
import { BsFillMicFill } from "react-icons/bs";

const StyledFooter = styled.div`
  display: grid;
  border: 1px solid;
  padding: 20px;
  box-sizing: border-box;
  grid-template-columns: 10% 10% 20% 20% 20% 10% 10%;
`;
const StyleCam = styled.div`
  margin: auto;
`;
const StyledBtn = styled.div`
  grid-column: 4 / 5;
  margin: auto;
`;

function WaitingFooter(props) {
  const handleReady = () => {
    setReadyFlag(!readyFlag);
    props.onClickReady();
  };

  const handleStart = () => {
    props.onClickStart();
  };

  const [readyFlag, setReadyFlag] = useState(true);

  const [localCamera, setLocalCamera] = useState(true);
  const [localMute, setLocalMute] = useState(true);

  const handleCameraClick = () => {
    console.log(localCamera ? "로컬 화면 끄기" : "로컬 화면 켜기");
    props.onClickCamera();
    setLocalCamera(!localCamera);
  };

  const handleMuteClick = () => {
    console.log(localMute ? "음성 끄기" : "음성 켜기");
    props.onClickMute();
    setLocalMute(!localMute);
  };

  return (
    <StyledFooter>
      <StyleCam onClick={handleCameraClick}>
        {localCamera ? (
          <BsFillCameraVideoFill size="50" />
        ) : (
          <BsFillCameraVideoOffFill size="50" />
        )}
      </StyleCam>
      <div onClick={handleMuteClick}>
        {localMute ? (
          <BsFillMicFill size="50" />
        ) : (
          <BsFillMicMuteFill size="50" />
        )}
      </div>
      <StyledBtn>
        {props.authority === "LEADER" && props.canStart && (
          <Button onClick={handleStart} size="lg">
            Start
          </Button>
        )}
        {props.authority === "LEADER" && !props.canStart && (
          <Button variant="secondary" size="lg">
            Start
          </Button>
        )}
        {props.authority !== "LEADER" && readyFlag && (
          <Button onClick={handleReady} size="lg">
            Ready
          </Button>
        )}

        {props.authority !== "LEADER" && !readyFlag && (
          <Button onClick={handleReady} variant="secondary" size="lg">
            Ready
          </Button>
        )}
      </StyledBtn>
    </StyledFooter>
  );
}
export default WaitingFooter;
