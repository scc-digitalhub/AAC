import {
    Datagrid,
    EditButton,
    ReferenceArrayField,
    Show,
    TabbedShowLayout,
    TextField,
    TopToolbar,
    useRecordContext,
    useTranslate,
} from 'react-admin';
import { PageTitle } from '../components/pageTitle';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { InspectButton } from '@dslab/ra-inspect-button';
import { Page } from '../components/page';
import { Box } from '@mui/material';
import { IdField } from '../components/IdField';

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
    const translate = useTranslate();
    if (!record) return null;

    return (
        <>
            <PageTitle text={record.name} secondaryText={record?.id} />
            <TabbedShowLayout syncWithLocation={false}>
                <TabbedShowLayout.Tab
                    label={translate('page.group.overview.title')}
                >
                    <TextField source="id" />
                    <TextField source="name" />
                    <TextField source="group" />
                    <TextField source="members" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab
                    label={translate('page.group.settings.title')}
                >
                    <TextField source="name" />
                    <TextField source="group" />
                    <TextField source="description" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab
                    label={translate('page.group.roles.title')}
                >
                    <TextField source="id" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab
                    label={translate('page.group.members.title')}
                >
                    <ReferenceArrayField
                        source="members"
                        reference="subjects"
                        label={false}
                    >
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
