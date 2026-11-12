import { InspectButton, InspectButtonProps } from '@dslab/ra-inspect-button';
import { useEffect, useState } from 'react';
import {
    useRecordContext,
    useResourceContext,
    useDataProvider,
} from 'react-admin';

/*
 * Inspect the record as returned by the backend, refetching it without
 * the view-level flattening (same approach as RefreshingExportButton)
 */
export const RefreshingInspectButton = (props: InspectButtonProps) => {
    const {
        record: recordProp,
        resource: resourceProp,
        ...rest
    } = props;
    const dataProvider = useDataProvider();
    const record = useRecordContext(props);
    const resource = useResourceContext(props);

    const [inspected, setInspected] = useState<any>(record);

    useEffect(() => {
        if (record && resource) {
            dataProvider.getOne(resource, { id: record.id }).then(data => {
                if (data?.data) {
                    setInspected(data.data);
                }
            });
        }
    }, [dataProvider, record, resource]);

    return (
        <InspectButton
            resource={resource}
            record={inspected}
            disabled={!inspected}
            {...rest}
        />
    );
};
