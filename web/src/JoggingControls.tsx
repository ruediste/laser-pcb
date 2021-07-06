import React from "react";
import { Badge, Button } from "react-bootstrap";
import { useHistory, useLocation } from "react-router";
import { post } from "./useData";
import WithData from "./WithData";
import queryString from "query-string";

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

function useQueryArgs<T>(name: string, def: T): [T, (value: T) => void] {
    const hist = useHistory();
    const location = useLocation();
    const parsedQuery = queryString.parse(location.search);
    const valueStr = parsedQuery[name];
    let value: T;
    if (valueStr === undefined)
        value = def;
    else
        value = JSON.parse(valueStr as string) as T;
    return [value, v => {
        hist.replace({ search: queryString.stringify({ [name]: JSON.stringify(v) }) });
    }];
}

export function JoggingControlsNoLoad({ state }: { state: SerialConnections }) {
    const [res, setRes] = useQueryArgs('jog', { value: 1 });
    return state.serialConnected ? <div>
        <Badge variant="success">Connected</Badge>

        <table>
            <tbody>
                <tr>
                    <td></td>
                    <td> <Button onClick={() => post('cncConnection/_jog').query({ direction: "Y+", distance: '' + res.value }).send()}>Y+</Button></td>
                    <td></td>
                    <td> <Button onClick={() => post('cncConnection/_jog').query({ direction: "Z+", distance: '' + res.value }).send()}>Z+</Button></td>
                    <td rowSpan={3}> {[0.01, 0.1, 1, 10, 100].map((v, idx) => <div key={idx} className="form-check">
                        <input className="form-check-input" type="radio" name="flexRadioDefault" id={"jogValue" + idx} checked={res.value === v} onChange={e => setRes({ value: v })} />
                        <label className="form-check-label" htmlFor={"jogValue" + idx}>{v}</label>
                    </div>)} </td>
                </tr>
                <tr>
                    <td> <Button onClick={() => post('cncConnection/_jog').query({ direction: "X-", distance: '' + res.value }).send()}>X-</Button></td>
                    <td></td>
                    <td> <Button onClick={() => post('cncConnection/_jog').query({ direction: "X+", distance: '' + res.value }).send()}>X+</Button></td>

                </tr>
                <tr>
                    <td></td>
                    <td> <Button onClick={() => post('cncConnection/_jog').query({ direction: "Y-", distance: '' + res.value }).send()}>Y-</Button></td>
                    <td></td>
                    <td> <Button onClick={() => post('cncConnection/_jog').query({ direction: "Z-", distance: '' + res.value }).send()}>Z-</Button></td>
                </tr>
            </tbody>
        </table>
        <table >
            <tbody>
                <tr>
                    <td>
                        <Button onClick={() => post('cncConnection/_autoHome').send()}>Auto Home</Button>{' '}
                    </td>
                    <td>
                        <Button onClick={() => post('cncConnection/_setLaser').query({ laserOn: 'true' }).send()}>Laser On</Button>
                    </td>
                    <td>
                        <Button onClick={() => post('cncConnection/_laserZ').send()}>Laser Z</Button>
                    </td>
                </tr>
                <tr>
                    <td></td>
                    <td>
                        <Button onClick={() => post('cncConnection/_setLaser').query({ laserOn: 'false' }).send()}>Laser Off</Button>
                    </td>
                    <td>
                        <Button onClick={() => post('cncConnection/_cameraZ').send()}>Camera Z</Button>
                    </td>
                </tr>
            </tbody>
        </table>

        {state.serialConnected ? <div>X:{'' + state.x} Y:{'' + state.y} Z: {'' + state.z}</div> : null}
    </div> : <div> <Badge variant="warn">Disconnected</Badge> </div>;
}

export default function JoggingControls() {
    return <WithData<SerialConnections> url="cncConnection"
        refreshMs={500}
        render={state => <JoggingControlsNoLoad state={state} />} />
}