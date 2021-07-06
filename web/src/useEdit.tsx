import React, { ReactNode, useEffect, useState } from 'react';
import { toast } from 'react-toastify';
import { post, request } from './useData';


interface EditFunctions<T> {
    update(value: Partial<T>): void;
    save(onSuccess?: () => void): void;
}
type UseEditResult<T> = [T | undefined, EditFunctions<T>];

interface UseEditProps<T>{
    url?: string;
    children: (args: UseEditResult<T>)=>ReactNode
}

export function UseEdit<T>(props: UseEditProps<T>){
    const args=useEdit<T>(props.url);
    return <React.Fragment>{props.children(args)}</React.Fragment>
}

export default function useEdit<T>(url?: string): UseEditResult<T> {
    const [state, setState] = useState<{ status: 'loading' } | { status: 'error', error: string } | { status: 'loaded', value?: T }>({ status: 'loading' });
    useEffect(() => {
        if (url === undefined) {
            setState({ status: 'loaded' });
            return;
        }
        request(url).method('GET').success(value => setState({ status: 'loaded', value })).error(error => setState({ status: 'error', error })).send();
    }, [url]);

    return [state.status === 'loaded' ? state.value : undefined, {
        update: (value) => state.status === 'loaded' ? setState({ status: 'loaded', value: state.value === undefined ? undefined : { ...(state.value), ...value } }) : null,
        save: (onSuccess?: () => void) => {
            if (url === undefined) {
                toast.error('url undefined');
                return;
            }
            if (state.status === 'loaded' && state.value !== undefined)
                post(url).body(state.value).success(() => {
                    toast.success("Saved");
                    if (onSuccess !== undefined)
                        onSuccess();
                }).error('Error during save').send();
        }
    }];
}
