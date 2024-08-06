import { Box, Grid, Stack } from '@mui/material';
import {
    EditButton,
    Labeled,
    RecordContextProvider,
    ReferenceArrayField,
    RichTextField,
    Show,
    TabbedShowLayout,
    TextField,
    TextInput,
    TopToolbar,
    useDataProvider,
    useRecordContext,
    useTranslate,
} from 'react-admin';
import { SectionTitle } from '../components/sectionTitle';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { InspectButton } from '@dslab/ra-inspect-button';
import { Page } from '../components/page';
import { AppTitle } from './AppTitle';
import { useEffect, useState } from 'react';
import { useRootSelector } from '@dslab/ra-root-selector';
import { grey } from '@mui/material/colors';
import { IdField } from '../components/IdField';

export const AppShow = () => {
    return (
        <Page>
            <Show
                actions={<ShowToolBarActions />}
                component={Box}
                queryOptions={{ meta: { flatten: ['roles', 'groups'] } }}
            >
                <AppTitle />
                <AppView />
            </Show>
        </Page>
    );
};

const AppView = () => {
    const record = useRecordContext();
    const translate = useTranslate();
    if (!record) return null;

    return (
        <TabbedShowLayout syncWithLocation={false}>
            <TabbedShowLayout.Tab label={translate('page.app.overview.title')}>
                <TextField source="name" />
                <TextField source="type" />
                <TextField source="clientId" />
                <TextField source="scopes" />
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
                <Grid container gap={1}>
                    <Grid item xs={12}>
                        <Labeled>
                            <IdField label="Client ID" source="clientId" />
                        </Labeled>
                    </Grid>
                    {record.configuration.clientSecret && (
                        <Grid item xs={12}>
                            <Labeled>
                                <IdField source="configuration.clientSecret" />
                            </Labeled>
                        </Grid>
                    )}
                    {record.configuration.jwks && (
                        <Grid item xs={12}>
                            <Labeled>
                                <IdField
                                    source="configuration.jwks"
                                    format={value =>
                                        value && value.length > 120
                                            ? value.substring(0, 120) + '...'
                                            : value
                                    }
                                />
                            </Labeled>
                        </Grid>
                    )}
                </Grid>
            </TabbedShowLayout.Tab>
            <TabbedShowLayout.Tab label="Api access">
                <SectionTitle
                    text={translate('page.app.scopes.header.title')}
                    secondaryText={translate('page.app.scopes.header.subtitle')}
                />
                <ReferenceArrayField source="scopes" reference="scopes" />
            </TabbedShowLayout.Tab>
            {}
            <TabbedShowLayout.Tab label="Endpoints">
                <SectionTitle
                    text={translate('page.app.endpoints.header.title')}
                    secondaryText={translate(
                        'page.app.endpoints.header.subtitle'
                    )}
                />
                <EndpointsView />
            </TabbedShowLayout.Tab>
        </TabbedShowLayout>
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

const EndpointsView = () => {
    const dataProvider = useDataProvider();
    const { root: realmId } = useRootSelector();
    const [config, setConfig] = useState<any>();

    useEffect(() => {
        dataProvider
            .invoke({ path: 'realms/' + realmId + '/well-known/oauth2' })
            .then(data => {
                if (data) {
                    setConfig(data);
                }
            });
    }, [dataProvider]);

    if (!config) return null;

    const fields = [
        'issuer',
        'authorization_endpoint',
        'token_endpoint',
        'jwks_uri',
        'userinfo_endpoint',
        'introspection_endpoint',
        'revocation_endpoint',
        'registration_endpoint',
        'end_session_endpoint',
    ];

    return (
        <RecordContextProvider value={config}>
            <Stack>
                {fields.map(field => {
                    return (
                        <Labeled key={'endpoints.' + field}>
                            <TextField source={field} />
                        </Labeled>
                    );
                })}
            </Stack>
        </RecordContextProvider>
    );
};
