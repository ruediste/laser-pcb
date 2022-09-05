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

type InputProps = {
    label: string,
    value: string,
    comment?: string,
    rows?: number,
    onChange: (value: string) => void
} & ( 
    {type: 'number', step?: number, min?: number, max?: number}
    | {type: 'text'}
    | {type: 'textarea', rows?: number}
    )



export function Input({type, label, value, comment, rows, onChange, ...others}: InputProps) {
    const id = useUniqueId();
    if (type === 'textarea')
        return <div className="form-group">
            <label htmlFor={id}>{label}</label>
            <textarea className="form-control" id={id} onChange={e => onChange(e.target.value)} value={value} {...others}>
            </textarea>
            {comment === undefined ? null :
                <small className="form-text text-muted">{comment}</small>
            }
        </div>;
    return <div className="form-group">
        <label htmlFor={id}>{label}</label>
        <input type={type} className="form-control" id={id} value={value} onChange={e => onChange(e.target.value)} {...others}/>
        {comment === undefined ? null :
            <small className="form-text text-muted">{comment}</small>
        }
    </div>;
}