import React from "react";
import { Badge, Button } from "react-bootstrap";
import Select from "react-dropdown-select";
import CameraView from "./CameraView";
import {JoggingControlsNoLoad, SerialConnections } from "./JoggingControls";
import { post } from "./useData";
import WithData from "./WithData";



export default function ControlComponent() {
    return <div>
        <WithData<SerialConnections> url="cncConnection"
            refreshMs={500}
            render={state => <div>
                CNC Connection
                <Select options={state.availableSerialConnections.map((x) => ({ label: x, value: x }))} values={state.selectedSerialConnection === undefined || state.selectedSerialConnection === null ? [] : [{ label: state.selectedSerialConnection, value: state.selectedSerialConnection }]}
                    onChange={(value) => { post('cncConnection/_setSerialConnection').query({ dev: value[0].value }).success('Serial Connection Changed').send() }} />
                <Button onClick={() => post('cncConnection/_connect').success("Connection initiated").send()}>Connect</Button>
                <Button onClick={() => post('cncConnection/_disconnect').success("Disconnected").send()}>Disconnect</Button>
               
               <JoggingControlsNoLoad state={state}/>

                Video Connection
                <Select options={state.availableVideoConnections.map((x) => ({ label: x, value: x }))} values={state.selectedVideoConnection === undefined || state.selectedVideoConnection === null ? [] : [{ label: state.selectedVideoConnection, value: state.selectedVideoConnection }]}
                    onChange={(value) => { post('cncConnection/_setVideoConnection').query({ dev: value[0].value }).success('Video Connection Changed').send() }} />

                <CameraView />
            </div>} />
    </div>;
}