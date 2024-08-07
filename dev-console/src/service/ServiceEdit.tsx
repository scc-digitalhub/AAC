import { Box, Typography } from '@mui/material';
import { Edit, ShowButton, TabbedForm, TextField, TextInput, TopToolbar, useRecordContext } from 'react-admin';
import StarBorderIcon from '@mui/icons-material/StarBorder';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { InspectButton } from '@dslab/ra-inspect-button';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { Page } from '../components/Page';
import { PageTitle } from '../components/PageTitle';
import { TabToolbar } from '../components/TabToolbar';

export const ServiceEdit = () => {
    return (
        <Page>
            <Edit
                actions={<EditToolBarActions />}
                mutationMode="pessimistic"
                component={Box}
            >
                <ServiceTabComponent />
            </Edit>
        </Page>
    );
};

const ServiceTabComponent = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <>
        <PageTitle text={record.name} secondaryText={record?.id} copy={true}/>
        <TabbedForm toolbar={<TabToolbar />} syncWithLocation={false}>
            <TabbedForm.Tab label="Overview">
            <TextField source="id" />
                    <TextField source="name" />
                    <TextField source="namespace" />
            </TabbedForm.Tab>
            <TabbedForm.Tab label="Settings">
                    <TextInput source="name" />
                    <TextInput source="description" />
                    <TextInput source="namespace" />
            </TabbedForm.Tab>
            <TabbedForm.Tab label="Scopes">
                </TabbedForm.Tab>
                <TabbedForm.Tab label="Claims"></TabbedForm.Tab>
        </TabbedForm>
    </>
    );
};

const EditToolBarActions = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <TopToolbar>
            <ShowButton />
            <InspectButton />
            <DeleteWithDialogButton />
            <ExportRecordButton />
        </TopToolbar>
    );
};
