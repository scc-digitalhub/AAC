import {
    EditButton,
    Show,
    TabbedShowLayout,
    TextField,
    TopToolbar,
    useRecordContext,
} from 'react-admin';
import { PageTitle } from '../components/pageTitle';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { InspectButton } from '@dslab/ra-inspect-button';
import { Page } from '../components/page';
import { Box } from '@mui/material';

export const GroupShow = () => {
    return (
        <Page>
            <Show actions={<ShowToolBarActions />} component={Box}>
                <GroupTabComponent />
            </Show>
        </Page>
    );
};

const GroupTabComponent = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <>
            <PageTitle text={record.name} secondaryText={record?.id} />
            <TabbedShowLayout syncWithLocation={false}>
                <TabbedShowLayout.Tab label="Overview">
                    <TextField source="id" />
                    <TextField source="name" />
                    <TextField source="group" />
                    <TextField source="members" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Settings">
                    <TextField source="name" />
                    <TextField source="group" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Roles">
                    <TextField source="id" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Members">
                    <TextField source="id" />
                    <TextField source="members" />
                </TabbedShowLayout.Tab>
            </TabbedShowLayout>
        </>
    );
};
const ShowToolBarActions = () => {
    const record = useRecordContext();
    if (!record) return null;
    return (
        <TopToolbar>
            <EditButton />
            <InspectButton />
            <DeleteWithDialogButton />
            <ExportRecordButton language="yaml" color="info" />
        </TopToolbar>
    );
};
