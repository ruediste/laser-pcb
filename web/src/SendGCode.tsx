import React from "react";
import { Button } from "react-bootstrap";
import { post } from "./useData";
import WithData from "./WithData";

interface SendGCodeStatus{
    lastCompletedGCodes : string[];
     inFlightGCodes: string[];
     nextGCodes : string[];
     state: {     x: number;
        y: number;
        z: number;
    }
}
export function SendGCode(){
    return <WithData<SendGCodeStatus> url="sendGCode"
    refreshMs={500}
    render={status => <React.Fragment>
        {status.state===null?null:<span>X: {status.state.x} Y: {status.state.y} Z: {status.state.z} </span>}
        <Button onClick={() => post('sendGCode/_cancel').send()}>Cancel</Button>
        <div style={{backgroundColor:'lightgreen'}}>
            {status.lastCompletedGCodes.map((x,idx)=><React.Fragment key={idx}>{x}<br/></React.Fragment>)}
        </div>
        <div style={{backgroundColor:'yellow'}}>
            {status.inFlightGCodes.map((x,idx)=><React.Fragment key={idx}>{x}<br/></React.Fragment>)}
        </div>
        <div style={{}}>
            {status.nextGCodes.map((x,idx)=><React.Fragment key={idx}>{x}<br/></React.Fragment>)}
        </div>
    </React.Fragment>}/>
}