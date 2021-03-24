import React from "react";
import { Badge, Button } from "react-bootstrap";
import Select from "react-dropdown-select";
import { post } from "./useData";
import WithData from "./WithData";

interface SerialConnections {
    selectedSerialConnection: string;
    availableSerialConnections: string[];
    connected: boolean;
    x?: number;
    y?: number;
    z?: number;
}
export default function ControlComponent() {
    return <div>
        <WithData<SerialConnections> url="cncConnection"
            refreshMs={500}
            render={state => <div>
                <Select options={state.availableSerialConnections.map((x) => ({ label: x, value: x }))} values={state.selectedSerialConnection === undefined || state.selectedSerialConnection === null ? [] : [{ label: state.selectedSerialConnection, value: state.selectedSerialConnection }]}
                    onChange={(value) => { post('cncConnection/_setSerialConnection').query({ dev: value[0].value }).success('Serial Connection Changed').send() }} />
                <Button onClick={() => post('cncConnection/_connect').success("Connection initiated").send()}>Connect</Button>
                <Button onClick={() => post('cncConnection/_disconnect').success("Disconnected").send()}>Disconnect</Button>
                {state.connected?<Badge variant="success">Connected</Badge>:<Badge variant="warn">Disconnected</Badge>}
                <Button onClick={() => post('cncConnection/_jog').query({direction:"X+"}).send()}>X+</Button>
                <Button onClick={() => post('cncConnection/_autoHome').send()}>Auto Home</Button>

                {state.connected?<div>X:{''+state.x} Y:{''+state.y} Z: {''+state.z}</div>: null}
                
            </div>} />
    </div>;
}