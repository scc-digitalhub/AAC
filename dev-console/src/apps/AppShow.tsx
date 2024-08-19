import { Box, Grid, Typography } from '@mui/material';
import {
    Button,
    DeleteWithConfirmButton,
    EditButton,
    IconButtonWithTooltip,
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
import DeveloperIcon from '@mui/icons-material/DeveloperMode';
import AdminIcon from '@mui/icons-material/AdminPanelSettings';
import { getAppIcon } from './utils';
import { AppIcon } from './AppIcon';

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

export const AppTitle = () => {
    const record = useRecordContext();
    if (!record) return null;

    const icon = record ? (
        getAppIcon(record.configuration?.applicationType, {
            fontSize: 'large',
            sx: { fontSize: '96px' },
            color: 'primary',
        })
    ) : (
        <AppIcon />
    );

    return (
        <ResourceTitle
            text={
                <Typography
                    variant="h4"
                    sx={{ pt: 0, pb: 1, textAlign: 'left' }}
                >
                    {record.name}{' '}
                    {record.authorities.find(
                        r => r.role === 'ROLE_DEVELOPER'
                    ) && (
                        <IconButtonWithTooltip
                            label={'ROLE_DEVELOPER'}
                            color="warning"
                        >
                            <DeveloperIcon fontSize="small" />
                        </IconButtonWithTooltip>
                    )}
                    {record.authorities.find(r => r.role === 'ROLE_ADMIN') && (
                        <IconButtonWithTooltip
                            label={'ROLE_ADMIN'}
                            color="warning"
                        >
                            <AdminIcon fontSize="small" />
                        </IconButtonWithTooltip>
                    )}
                </Typography>
            }
            icon={icon}
        />
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
