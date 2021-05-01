import React from "react";
import WithData from "./WithData";

interface SendGCodeStatus{
    lastCompletedGCodes : string[];
     inFlightGCodes: string[];
     nextGCodes : string[];
}
export function SendGCode(){
    return <WithData<SendGCodeStatus> url="sendGCode"
    refreshMs={500}
    render={status => <React.Fragment>
        <div style={{color:'lightgreen'}}>
            {status.lastCompletedGCodes.map((x,idx)=><React.Fragment key={idx}>{x}<br/></React.Fragment>)}
        </div>
        <div style={{color:'yellow'}}>
            {status.inFlightGCodes.map((x,idx)=><React.Fragment key={idx}>{x}<br/></React.Fragment>)}
        </div>
        <div style={{}}>
            {status.nextGCodes.map((x,idx)=><React.Fragment key={idx}>{x}<br/></React.Fragment>)}
        </div>
    </React.Fragment>}/>
}