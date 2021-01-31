import useData, { UseDataArgs } from "./useData";

export default function WithData<T>(props: UseDataArgs & { render: (value: T, trigger: () => void) => JSX.Element|null }) {
    const { render, ...otherProps } = props;
    const data = useData<T>(otherProps);

    if (data.state === 'error') {
        return <div className="alert alert-danger" role="alert">
            Error loading data: {data.error}
        </div>
    }
    if (data.state === 'loading') {
        return <div className="spinner-border" role="status">
            <span className="sr-only">Loading...</span>
        </div>
    }
    if (data.state === 'success') {
        return render(data.value, data.trigger);
    }
    throw Error('unsupported state');
}