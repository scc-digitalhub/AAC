import { Box } from '@mui/material';
import {
    ArrayField,
    EditButton,
    ReferenceField,
    Show,
    TabbedShowLayout,
    TextField,
    TopToolbar,
    useRecordContext,
    useTranslate,
} from 'react-admin';
import { InspectButton } from '@dslab/ra-inspect-button';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { PageTitle } from '../components/pageTitle';
import { Page } from '../components/page';
import { ActiveButton } from './activeButton';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { JsonSchemaField } from '@dslab/ra-jsonschema-input';
import { schemaOAuthClient, uiSchemaOAuthClient } from '../common/schemas';

export const UserShow = () => {
    return (
        <Page>
            <Show actions={<ShowToolBarActions />} component={Box}>
                <UserTabComponent />
            </Show>
        </Page>
    );
};

const UserTabComponent = () => {
    const record = useRecordContext();
    const translate = useTranslate();
    if (!record) return null;

    return (
        <>
            <PageTitle text={record.username} secondaryText={record?.id} />
            <TabbedShowLayout syncWithLocation={false}>
                <TabbedShowLayout.Tab
                    label={translate('page.user.overview.title')}
                >
                    <TextField source="username" />
                    <TextField source="email" />
                    <TextField source="subjectId" />
                    <TextField source="roles" />
                    <TextField source="permissions" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab
                    label={translate('page.user.account.title')}
                >
                    <JsonSchemaField
                        source="account"
                        schema={schemaOAuthClient}
                        uiSchema={uiSchemaOAuthClient}
                    />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab
                    label={translate('page.user.audit.title')}
                >
                    <TextField source="id" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label={translate('page.user.apps.title')}>
                    <TextField source="id" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab
                    label={translate('page.user.groups.title')}
                >
                    <ArrayField source="groups" >
                        {/* <ReferenceField source="groupId" reference="groups" label="Group" /> */}
                        <TextField source="name" />
                     </ArrayField>
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab
                    label={translate('page.user.roles.title')}
                >
                    <TextField source="id" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab
                    label={translate('page.user.attributes.title')}
                >
                    <TextField source="id" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label={translate('page.user.tos.title')}>
                    <TextField source="id" />
                </TabbedShowLayout.Tab>
            </TabbedShowLayout>
        </>
    );
};

const ShowToolBarActions = () => {
    const record = useRecordContext();
    if (!record) return null;
    let body = JSON.stringify(record, null, '\t');
    return (
        <TopToolbar>
            <ActiveButton />
            <EditButton />
            <InspectButton />
            <DeleteWithDialogButton />
            <ExportRecordButton language="yaml" color="info" />
        </TopToolbar>
    );
};
