import { Box, Grid } from '@mui/material';
import {
    DeleteWithConfirmButton,
    EditButton,
    Labeled,
    ReferenceArrayField,
    ReferenceManyField,
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
import { Page } from '../components/Page';
import { IdField } from '../components/IdField';
import { AppEndpointsView } from './AppEndpoints';
import { TestDialogButton } from './TestDialog';
import { ResourceTitle } from '../components/ResourceTitle';
import { AuditListView } from '../audit/AuditList';

export const AppShow = () => {
    return (
        <Page>
            <Show
                actions={<ShowToolBarActions />}
                component={Box}
                queryOptions={{ meta: { flatten: ['roles', 'groups'] } }}
            >
                <ResourceTitle />
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
            <TabbedShowLayout.Tab label="tab.overview">
                <TextField source="name" />
                <TextField source="type" />
                <TextField source="clientId" />
                <TextField source="scopes" />
                <ReferenceArrayField source="groups" reference="groups" />
                <ReferenceArrayField source="roles" reference="roles" />
            </TabbedShowLayout.Tab>
            <TabbedShowLayout.Tab label="tab.credentials">
                <SectionTitle
                    text="page.apps.credentials.header.title"
                    secondaryText="page.apps.credentials.header.subtitle"
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
            <TabbedShowLayout.Tab label="tab.api_access">
                <SectionTitle
                    text="page.apps.scopes.header.title"
                    secondaryText="page.apps.scopes.header.subtitle"
                />
                <ReferenceArrayField source="scopes" reference="scopes" />
            </TabbedShowLayout.Tab>
            <TabbedShowLayout.Tab label="tab.endpoints">
                <SectionTitle
                    text="page.apps.endpoints.header.title"
                    secondaryText="page.apps.endpoints.header.subtitle"
                />
                <AppEndpointsView />
            </TabbedShowLayout.Tab>
            <TabbedShowLayout.Tab label="tab.audit">
                <SectionTitle
                    text="page.apps.audit.title"
                    secondaryText="page.apps.audit.subTitle"
                />
                <ReferenceManyField reference="audit" target="principal">
                    <AuditListView />
                </ReferenceManyField>
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
