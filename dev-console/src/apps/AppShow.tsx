import { Box, Grid } from '@mui/material';
import {
    DeleteWithConfirmButton,
    EditButton,
    Labeled,
    ReferenceArrayField,
    Show,
    TabbedShowLayout,
    TextField,
    TopToolbar,
    useRecordContext,
    useTranslate,
} from 'react-admin';
import { SectionTitle } from '../components/sectionTitle';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { InspectButton } from '@dslab/ra-inspect-button';
import { Page } from '../components/page';
import { AppTitle } from './AppTitle';
import { IdField } from '../components/IdField';
import { AppEndpointsView } from './AppEndpoints';
import { TestDialogButton } from './TestDialog';

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
                <ReferenceArrayField source="groups" reference="groups" />
                <ReferenceArrayField source="roles" reference="roles" />
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
                <AppEndpointsView />
            </TabbedShowLayout.Tab>
        </TabbedShowLayout>
    );
};

const ShowToolBarActions = () => (
    <TopToolbar>
        <TestDialogButton />
        <EditButton />
        <InspectButton />
        <DeleteWithConfirmButton />
        <ExportRecordButton language="yaml" color="info" />
    </TopToolbar>
);
