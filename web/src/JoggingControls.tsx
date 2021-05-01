import React from "react";
import { Badge, Button } from "react-bootstrap";
import Select from "react-dropdown-select";
import CameraView from "./CameraView";
import { post } from "./useData";
import WithData from "./WithData";

export interface SerialConnections {
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

export function JoggingControlsNoLoad({state}: {state: SerialConnections}) {
    return  state.serialConnected ? <div>
            <Badge variant="success">Connected</Badge>
            <table>
                <tbody>
                    <tr>
                        <td></td>
                        <td> <Button onClick={() => post('cncConnection/_jog').query({ direction: "Y+" }).send()}>Y+</Button></td>
                        <td></td>
                        <td> <Button onClick={() => post('cncConnection/_jog').query({ direction: "Z+" }).send()}>Z+</Button></td>
                    </tr>
                    <tr>
                        <td> <Button onClick={() => post('cncConnection/_jog').query({ direction: "X-" }).send()}>X-</Button></td>
                        <td></td>
                        <td> <Button onClick={() => post('cncConnection/_jog').query({ direction: "X+" }).send()}>X+</Button></td>

                    </tr>
                    <tr>
                        <td></td>
                        <td> <Button onClick={() => post('cncConnection/_jog').query({ direction: "Y-" }).send()}>Y-</Button></td>
                        <td></td>
                        <td> <Button onClick={() => post('cncConnection/_jog').query({ direction: "Z-" }).send()}>Z-</Button></td>
                    </tr>
                </tbody>
            </table>
            <Button onClick={() => post('cncConnection/_jog').query({ direction: "X+" }).send()}>X+</Button>
            <Button onClick={() => post('cncConnection/_autoHome').send()}>Auto Home</Button>

            {state.serialConnected ? <div>X:{'' + state.x} Y:{'' + state.y} Z: {'' + state.z}</div> : null}
        </div> : <div> <Badge variant="warn">Disconnected</Badge> </div>;
}

export default function JoggingControls() {
    return <WithData<SerialConnections> url="cncConnection"
        refreshMs={500}
        render={state => <JoggingControlsNoLoad state={state}/>}/>
}