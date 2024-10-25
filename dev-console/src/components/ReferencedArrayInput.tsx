import {
    AutocompleteArrayInput,
    Button,
    ReferenceArrayInput,
    Toolbar,
    useDataProvider,
    useGetList,
    useNotify,
    useRecordContext,
    useRefresh,
    useResourceContext,
} from 'react-admin';
import { useRootSelector } from '@dslab/ra-root-selector';
import { useWatch } from 'react-hook-form';
import ContentSave from '@mui/icons-material/Save';

export const ReferencedArrayInput = (props: {
    reference: string;
    source: string;
    record?: any;
    resource?: string;
    label?: string;
    helperText?: string;
}) => {
    const { reference, source, label, helperText } = props;
    const record = useRecordContext(props);
    const resource = useResourceContext(props);
    const { root: realmId } = useRootSelector();
    const dataProvider = useDataProvider();
    const notify = useNotify();
    const refresh = useRefresh();

    //fetch related to resolve relations
    const { data } = useGetList(reference, {
        pagination: { page: 1, perPage: 100 },
        sort: { field: 'name', order: 'ASC' },
    });

    if (!record) return null;

    //inflate back flattened fields
    const field = useWatch({ name: source, defaultValue: [] });
    const handleSave = e => {
        e.stopPropagation();
        if (
            dataProvider &&
            record &&
            data !== undefined &&
            field !== undefined
        ) {
            const value = data.filter(g => field.includes(g.id));

            dataProvider
                .invoke({
                    path:
                        resource +
                        '/' +
                        realmId +
                        '/' +
                        record.id +
                        '/' +
                        reference,
                    body: JSON.stringify(value),
                    options: {
                        method: 'PUT',
                    },
                })
                .then(() => {
                    notify('ra.notification.updated', {
                        messageArgs: { smart_count: 1 },
                    });
                    refresh();
                });
        }
    };

    return (
        <>
            <ReferenceArrayInput
                source={source}
                reference={reference}
                sort={{ field: 'name', order: 'ASC' }}
            >
                <AutocompleteArrayInput label={label} helperText={helperText} />
            </ReferenceArrayInput>
            <Toolbar sx={{ width: '100%' }}>
                <Button
                    label="ra.action.save"
                    startIcon={<ContentSave />}
                    onClick={handleSave}
                    variant="contained"
                    size="medium"
                />
            </Toolbar>
        </>
    );
};
