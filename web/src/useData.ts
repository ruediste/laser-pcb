import { useEffect, useRef, useState } from 'react';
import useDeepCompareEffect from 'use-deep-compare-effect'
import { toast } from 'react-toastify';


/** convert the object to a query string, prefixed with an ampersand (&) if non-empty */
const toQueryString = (obj: { [key: string]: string }) => {
    let entries = Object.entries(obj);
    if (entries.length === 0)
        return '';

    let params = new URLSearchParams();
    for (const [key, value] of entries) {
        params.append(key, value);
    }
    return '?' + params.toString();
};


export const baseUrl = window.location.protocol + "//" + window.location.hostname + ":8080/";

export class Request {
    constructor(public url: string) { }

    _body?: any;
    body(value: any) { this._body = value; return this; }

    _bodyRaw?: any;
    bodyRaw(value: any) { this._bodyRaw = value; return this; }

    _method: string = "GET";
    method(value: 'GET' | 'POST' | 'DELETE') { this._method = value; return this; }

    _query?: { [key: string]: string };
    query(value: { [key: string]: string }) { this._query = value; return this; }

    _success?: ((data: any) => any) | string
    success(value: ((data: any) => any) | string) { this._success = value; return this; }

    _error?: ((error: any) => any) | string
    error(value: ((error: any) => any) | string) { this._error = value; return this; }

    private handleSuccess(data: any) {
        if (typeof this._success === 'string')
            toast.success(this._success);
        else if (this._success !== undefined)
            this._success(data);
    }

    private handleError(error: any) {
        if (this._error === undefined)
            toast.error(error.toString());
        else if (typeof this._error === 'string')
            toast.error(this._error + ": " + error.toString());
        else
            this._error(error);
    }

    private buildUrl() { return baseUrl + this.url + (this._query === undefined ? "" : toQueryString(this._query)) }

    upload(onProgress: (args: { loaded: number, total: number }) => void) {
        const xhr = new XMLHttpRequest();
        xhr.onload = (e) => this.handleSuccess(xhr.response);
        xhr.upload.onerror = (e) => this.handleError(e.type);
        xhr.onerror = (e) => this.handleError(e.type);
        xhr.upload.onprogress = (e) => onProgress(e);
        xhr.open('POST', this.buildUrl(), true);
        xhr.setRequestHeader('Content-Type', 'application/octet-stream')
        xhr.send(this._bodyRaw);
    }

    send() {
        const init: RequestInit = { method: this._method };
        if (this._body !== undefined) {
            init.body = JSON.stringify(this._body);
            init.headers = { 'Content-Type': 'application/json' };
        }
        if (this._bodyRaw !== undefined) {
            init.body = this._bodyRaw;
            init.headers = { 'Content-Type': 'application/octet-stream' };
        }


        let tmp = fetch(this.buildUrl(), init)
            .then(r => {
                if (!r.ok) {
                    throw Error(r.status + ": " + r.statusText);
                }
                if (this._success === undefined || typeof this._success === 'string')
                    return null;
                if (r.headers.get('Content-Type') === 'application/json')
                    return r.json();
                return null;
            });
        if (this._success !== undefined)
            tmp = tmp.then(data => this.handleSuccess(data));
        tmp.catch(error => this.handleError(error));
    }
}

export function request(url: string) {
    return new Request(url);

}
export function post(url: string) {
    return new Request(url).method('POST');
}


export class RefreshTrigger {
    private callbacks: Set<() => void> = new Set();
    add(callback: () => void): void { this.callbacks.add(callback) }
    remove(callback: () => void): void { this.callbacks.delete(callback) }
    trigger(): void { this.callbacks.forEach(x => x()) }
}

export function useRefreshTrigger(): RefreshTrigger {
    return useRef(new RefreshTrigger()).current;
}

export interface UseDataArgs {
    url: string;
    queryParams?: { [key: string]: string };
    trigger?: RefreshTrigger
    refreshMs?: number
}

export type Data<T> = { trigger: () => void } & ({ state: "loading" } | { state: "success", value: T } | { state: "error", error: string });

class DataLoader<T> {
    closed = false;

    timeout?: NodeJS.Timeout;

    constructor(public args: UseDataArgs, public setData: ((data: Data<T>) => void)) {
    }

    performLoad = () => {
        console.log('DataLoader: performLoad()', this.args);
        if (this.closed)
            return;
        if (this.timeout !== undefined) {
            clearTimeout(this.timeout);
            this.timeout = undefined;
        }
        fetch(baseUrl + this.args.url + toQueryString(this.args.queryParams ?? {}), {})
            .then(r => {
                if (!r.ok)
                    throw Error(r.statusText);
                return r.json();
            })
            .then(d => {
                if (this.closed)
                    return;
                this.setData({ state: 'success', value: d as T, trigger: this.performLoad });
                if (this.args.refreshMs !== undefined)
                    this.timeout = setTimeout(this.performLoad, this.args.refreshMs);

            })
            .catch(error => {
                if (this.closed) return;
                this.setData({ state: 'error', error: error.toString(), trigger: this.performLoad });
                if (this.args.refreshMs !== undefined)
                    this.timeout = setTimeout(this.performLoad, this.args.refreshMs);

            });
    }

    close() {
        this.closed = true;
        if (this.timeout !== undefined) {
            clearTimeout(this.timeout);
            this.timeout = undefined;
        }
    }
}

export default function useData<T>(args: UseDataArgs): Data<T> {
    let dataLoaderRef = useRef<DataLoader<T>>();
    const [data, setData] = useState<Data<T>>({ state: 'loading', trigger: () => dataLoaderRef.current?.performLoad() });

    useEffect(() => {
        const trigger = args.trigger;
        if (trigger !== undefined) {
            const performLoad = () => dataLoaderRef.current?.performLoad();
            trigger.add(performLoad);
            return () => trigger.remove(performLoad);
        }
    }, [args.trigger]);

    useDeepCompareEffect(() => {
        const dataLoader = new DataLoader<T>(args, null as any);
        dataLoader.setData = setData;
        dataLoader.args = args;
        dataLoader.performLoad();
        dataLoaderRef.current = dataLoader;
        return () => dataLoader.close();
    }, [args]);
    return data;
}