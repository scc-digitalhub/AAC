import { Box } from '@mui/material';
import {
    EditButton,
    ReferenceArrayField,
    ReferenceArrayInput,
    RichTextField,
    Show,
    SimpleForm,
    TabbedShowLayout,
    TextField,
    TopToolbar,
    useEditContext,
    useNotify,
    useRecordContext,
    useRefresh,
    useTranslate,
} from 'react-admin';
import { PageTitle } from '../components/pageTitle';
import { SectionTitle } from '../components/sectionTitle';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { InspectButton } from '@dslab/ra-inspect-button';
import { JsonSchemaField } from '@dslab/ra-jsonschema-input';
import { schemaOAuthClient, uiSchemaOAuthClient } from '../common/schemas';
import { Page } from '../components/page';

export const AppShow = () => {
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
    const translate = useTranslate();
    if (!record) return null;

    return (
        <>
            <PageTitle text={record.name} secondaryText={record?.id} />
            <TabbedShowLayout syncWithLocation={false}>
                <TabbedShowLayout.Tab
                    label={translate('page.app.overview.title')}
                >
                    <TextField source="name" />
                    <TextField source="type" />
                    <TextField source="clientId" />
                    <TextField source="scopes" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab
                    label={translate('page.app.settings.title')}
                >
                    <TextField source="name" />
                    <TextField source="description" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab
                    label={translate('page.app.configuration.title')}
                >
                    <SectionTitle
                        text={translate('page.app.configuration.header.title')}
                        secondaryText={translate(
                            'page.app.configuration.header.subtitle'
                        )}
                    />
                    <OAuthJsonSchemaForm />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab
                    label={translate('page.app.credentials.title')}
                >
                    <SectionTitle
                        text={translate('page.app.credentials.header.title')}
                        secondaryText={translate(
                            'page.app.credentials.header.subtitle'
                        )}
                    />
                    <TextField label="Client ID" source="clientId" />
                    {record.configuration.clientSecret && (
                        <TextField
                            label="Client Key Set (JWKS)"
                            source="configuration.clientSecret"
                        />
                    )}
                    {record.configuration.jwks && (
                        <RichTextField
                            label="Client Secret"
                            source="configuration.jwks"
                        />
                    )}
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Providers">
                    <ReferenceArrayField source="providers" reference="idps" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Scopes">
                    {record.configuration.scopes && (
                        <ReferenceArrayField
                            source="scopes"
                            reference="scopes"
                        />
                    )}
                </TabbedShowLayout.Tab>
            </TabbedShowLayout>
        </>
    );
};
const OAuthJsonSchemaForm = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <JsonSchemaField
            source="configuration"
            schema={schemaOAuthClient}
            uiSchema={uiSchemaOAuthClient}
        />
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
