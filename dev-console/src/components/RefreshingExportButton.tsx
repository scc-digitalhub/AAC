import {
    ExportRecordButton,
    ExportRecordButtonProps,
} from '@dslab/ra-export-record-button';
import { useEffect, useState } from 'react';
import {
    useRecordContext,
    useResourceContext,
    useDataProvider,
} from 'react-admin';

export const RefreshingExportButton = (props: ExportRecordButtonProps) => {
    const {
        language = 'yaml',
        color = 'info',
        record: recordProp,
        resource: resourceProp,
        ...rest
    } = props;
    const dataProvider = useDataProvider();
    const record = useRecordContext(props);
    const resource = useResourceContext(props);

    const [exported, setExported] = useState<any>(record);

    useEffect(() => {
        if (record && resource) {
            dataProvider.getOne(resource, { id: record.id }).then(data => {
                if (data?.data) {
                    setExported(data.data);
                }
            });
        }
    }, [dataProvider, record, resource]);

    return (
        <ExportRecordButton
            language={language}
            color={color}
            resource={resource}
            record={exported}
            disabled={!exported}
            {...rest}
        />
    );
};
