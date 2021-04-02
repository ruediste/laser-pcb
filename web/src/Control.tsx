import React, { useEffect, useState } from "react";
import { Badge, Button } from "react-bootstrap";
import Select from "react-dropdown-select";
import { post, baseUrl } from "./useData";
import WithData from "./WithData";

interface SerialConnections {
    selectedSerialConnection: string;
    availableSerialConnections: string[];
    serialConnected: boolean;


    x?: number;
    y?: number;
    z?: number;

    selectedVideoConnection: string;
    availableVideoConnections: string[];
    videoConnected: boolean;
}
export default function ControlComponent() {
    const [frameCount, setFrameCount] = useState(0);
    // useEffect(() => {
    //     const timer = setTimeout(() => setFrameCount(c => c + 1), 100);
    //     // Clear timeout if the component is unmounted
    //     return () => clearTimeout(timer);
    // });
    return <div>
        <WithData<SerialConnections> url="cncConnection"
            refreshMs={500}
            render={state => <div>
                CNC Connection
                <Select options={state.availableSerialConnections.map((x) => ({ label: x, value: x }))} values={state.selectedSerialConnection === undefined || state.selectedSerialConnection === null ? [] : [{ label: state.selectedSerialConnection, value: state.selectedSerialConnection }]}
                    onChange={(value) => { post('cncConnection/_setSerialConnection').query({ dev: value[0].value }).success('Serial Connection Changed').send() }} />
                <Button onClick={() => post('cncConnection/_connect').success("Connection initiated").send()}>Connect</Button>
                <Button onClick={() => post('cncConnection/_disconnect').success("Disconnected").send()}>Disconnect</Button>
                {state.serialConnected ? <Badge variant="success">Connected</Badge> : <Badge variant="warn">Disconnected</Badge>}
                <Button onClick={() => post('cncConnection/_jog').query({ direction: "X+" }).send()}>X+</Button>
                <Button onClick={() => post('cncConnection/_autoHome').send()}>Auto Home</Button>

                {state.serialConnected ? <div>X:{'' + state.x} Y:{'' + state.y} Z: {'' + state.z}</div> : null}

                Video Connection
                <Select options={state.availableVideoConnections.map((x) => ({ label: x, value: x }))} values={state.selectedVideoConnection === undefined || state.selectedVideoConnection === null ? [] : [{ label: state.selectedVideoConnection, value: state.selectedVideoConnection }]}
                    onChange={(value) => { post('cncConnection/_setVideoConnection').query({ dev: value[0].value }).success('Video Connection Changed').send() }} />

                <img src={baseUrl + 'cncConnection/frame.jpg?c='+frameCount} onLoad={()=>setFrameCount(c => c + 1)}/>
            </div>} />
    </div>;
}