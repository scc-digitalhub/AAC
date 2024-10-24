import {
    ArrayField,
    ChipField,
    Datagrid,
    DeleteWithConfirmButton,
    FunctionField,
    Link,
    ReferenceField,
    ReferenceManyField,
    SimpleList,
    SingleFieldList,
    TextField,
    useCreatePath,
    useDataProvider,
    useRecordContext,
} from 'react-admin';
import { DataGridBlankHeader } from '../components/DataGridBlankHeader';
import { Stack } from '@mui/material';
import { AppNameField } from '../apps/AppList';
import { IdField } from '../components/IdField';

export const ConnectedApps = () => {
    const record = useRecordContext();
    const dataProvider = useDataProvider();
    const createPath = useCreatePath();
    if (!record) return null;

    return (
        <ReferenceManyField reference="connectedapps" target="subjectId">
            <Datagrid
                bulkActionButtons={false}
                rowClick={false}
                header={<DataGridBlankHeader />}
            >
                <ReferenceField source="clientId" reference="apps">
                    <AppNameField source="name" />
                </ReferenceField>
                <IdField source="clientId" />
                <ArrayField source="scopes">
                    <SingleFieldList
                        component={Stack}
                        direction={'row'}
                        linkType={false}
                    >
                        <ChipField source="scope" />
                    </SingleFieldList>
                </ArrayField>
                <DeleteWithConfirmButton redirect={false} />
            </Datagrid>
        </ReferenceManyField>
    );
};
