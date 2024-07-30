import { Box } from '@mui/material';
import {
    Datagrid,
    EditButton,
    ReferenceArrayField,
    Show,
    ShowButton,
    TabbedShowLayout,
    TextField,
    TopToolbar,
    useRecordContext,
} from 'react-admin';
import { PageTitle } from '../components/pageTitle';
import { Page } from '../components/page';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { InspectButton } from '@dslab/ra-inspect-button';
import { IdField } from '../components/IdField';

export const RoleShow = () => {
    return (
        <Page>
            <Show actions={<ShowToolBarActions />} component={Box}>
                <AppTabComponent />
            </Show>
        </Page>
    );
};

const AppTabComponent = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <>
            <PageTitle text={record.name} secondaryText={record?.id} />

            <TabbedShowLayout syncWithLocation={false}>
                <TabbedShowLayout.Tab label="Overview">
                    <TextField source="id" />
                    <TextField source="authority" />
                    <TextField source="authority" />
                    <TextField source="namespace" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Settings"></TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Permission"></TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Subjects">
             
                    <ReferenceArrayField source="subjects" reference="subjects" label={false}>
                        <Datagrid bulkActionButtons={false}>
                            <TextField source="name" />
                            <TextField source="type" />
                            <IdField source="id" />
                        </Datagrid>
                    </ReferenceArrayField>
                </TabbedShowLayout.Tab>
            </TabbedShowLayout>
        </>
    );
};

const ShowToolBarActions = () => {
    return (
        <TopToolbar>
            <EditButton />
            <InspectButton />
            <DeleteWithDialogButton />
            <ExportRecordButton />
        </TopToolbar>
    );
};
