import { Box, Typography } from '@mui/material';
import { Edit, ShowButton, TabbedForm, TextField, TopToolbar, useRecordContext } from 'react-admin';
import StarBorderIcon from '@mui/icons-material/StarBorder';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { InspectButton } from '@dslab/ra-inspect-button';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { Page } from '../components/page';
import { PageTitle } from '../components/pageTitle';
import { TabToolbar } from '../components/TabToolbar';

export const RoleEdit = () => {
    return (
        <Page>
            <Edit actions={<EditToolBarActions />} mutationMode="pessimistic" component={Box}>
                <RoleTabComponent />
            </Edit>
        </Page>
    );
};

const RoleTabComponent = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <>
            <PageTitle text={record.name} secondaryText={record?.id} copy={true}/>
            <TabbedForm toolbar={<TabToolbar />}>
                <TabbedForm.Tab label="Overview">
                    <TextField source="id" />
                    <TextField source="authority" />
                    <TextField source="authority" />
                    <TextField source="namespace" />
                </TabbedForm.Tab>
                <TabbedForm.Tab label="Settings"></TabbedForm.Tab>
                <TabbedForm.Tab label="Permission"></TabbedForm.Tab>
                <TabbedForm.Tab label="Subjects"></TabbedForm.Tab>
            </TabbedForm>
        </>
    )
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
