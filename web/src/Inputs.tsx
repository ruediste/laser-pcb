import { useState } from 'react';
let nextUniqueId: number = 0;
function useUniqueId() {
    const [id] = useState(() => '' + (nextUniqueId++));
    return id;
}

interface InputCheckProps {
    style?: React.CSSProperties
    label: string;
    value: boolean;
    onChange: (value: boolean) => void
}
export function InputCheck(props: InputCheckProps) {
    const id = useUniqueId();
    return <div className="form-check" style={props.style}>
        <input className="form-check-input" type="checkbox" value="" id={id} checked={props.value} onChange={e => props.onChange(e.target.checked)} />
        <label className="form-check-label" htmlFor={id}>
            {props.label}
        </label>
    </div>
}

interface InputProps {
    type: 'number'|'text',
    label: string,
    value: string,
    onChange: (value: string) => void
}

export function Input(props: InputProps) {
    const id = useUniqueId();
    return <div className="form-group">
        <label htmlFor={id}>{props.label}</label>
        <input type={props.type} className="form-control" id={id} value={props.value} onChange={e => props.onChange(e.target.value)} />
    </div>;
}