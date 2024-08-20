import { Avatar, Box, Typography } from '@mui/material';
import {
    ArrayField,
    Datagrid,
    EditButton,
    IconButtonWithTooltip,
    ReferenceArrayField,
    ReferenceField,
    ReferenceManyField,
    Show,
    TabbedShowLayout,
    TextField,
    TopToolbar,
    useRecordContext,
    useRefresh,
    useTranslate,
} from 'react-admin';
import { InspectButton } from '@dslab/ra-inspect-button';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { PageTitle } from '../components/PageTitle';
import { Page } from '../components/Page';
import { ActiveButton } from './activeButton';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { JsonSchemaField } from '@dslab/ra-jsonschema-input';
import { IdField } from '../components/IdField';
import { SectionTitle } from '../components/sectionTitle';
import { AuthoritiesDialogButton } from '../components/AuthoritiesDialog';
import { RefreshingExportButton } from '../components/RefreshingExportButton';
import { ResourceTitle } from '../components/ResourceTitle';

import { grey } from '@mui/material/colors';
import { isValidElement } from 'react';
import { AuditListView } from '../audit/AuditList';
import { ConnectedApps } from './ConnectedApps';
import { useRootSelector } from '@dslab/ra-root-selector';
import { AdminIcon, DeveloperIcon } from '../developers/DeveloperIcon';

export const UserShow = () => {
    return (
        <Page>
            <Show
                actions={<ShowToolBarActions />}
                component={Box}
                queryOptions={{ meta: { flatten: ['roles', 'groups'] } }}
            >
                <ResourceTitle text={<UserTitle />} icon={<UserAvatar />} />
                <UserView />
            </Show>
        </Page>
    );
};

const UserTitle = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <Typography variant="h4" sx={{ pt: 0, pb: 1, textAlign: 'left' }}>
            {record.username}{' '}
            {record.authorities.find(r => r.role === 'ROLE_DEVELOPER') && (
                <IconButtonWithTooltip label={'ROLE_DEVELOPER'} color="warning">
                    <DeveloperIcon fontSize="small" />
                </IconButtonWithTooltip>
            )}
            {record.authorities.find(r => r.role === 'ROLE_ADMIN') && (
                <IconButtonWithTooltip label={'ROLE_ADMIN'} color="warning">
                    <AdminIcon fontSize="small" />
                </IconButtonWithTooltip>
            )}
        </Typography>
    );
};

const UserAvatar = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <Avatar sx={{ mt: 1, height: '96px', width: '96px', fontSize: '48px' }}>
            {record.username.toUpperCase().substring(0, 2)}
        </Avatar>
    );
};

const UserView = () => {
    const record = useRecordContext();
    const { root: realmId } = useRootSelector();
    const translate = useTranslate();
    if (!record) return null;

    return (
        <TabbedShowLayout syncWithLocation={false}>
            <TabbedShowLayout.Tab label={translate('page.user.overview.title')}>
                <TextField source="username" />
                <TextField source="email" />
                <TextField source="subjectId" />
                <ReferenceArrayField source="groups" reference="groups" />
                <ReferenceArrayField source="roles" reference="roles" />
            </TabbedShowLayout.Tab>
            <TabbedShowLayout.Tab label={translate('page.user.account.title')}>
                <SectionTitle
                    text={translate('page.user.account.title')}
                    secondaryText={translate('page.user.account.subTitle')}
                />
                <ArrayField source="identities">
                    <Datagrid bulkActionButtons={false}>
                        <TextField source="username" />
                        <TextField source="emailAddress" />
                        <TextField source="provider" />
                    </Datagrid>
                </ArrayField>
            </TabbedShowLayout.Tab>
            <TabbedShowLayout.Tab label={translate('page.user.apps.title')}>
                <SectionTitle
                    text={translate('page.user.apps.title')}
                    secondaryText={translate('page.user.apps.subTitle')}
                />
                <ConnectedApps />
            </TabbedShowLayout.Tab>
            <TabbedShowLayout.Tab label={translate('page.user.groups.title')}>
                <SectionTitle
                    text={translate('page.user.groups.title')}
                    secondaryText={translate('page.user.groups.subTitle')}
                />
                {/* <ArrayField source="groups">
                    <TextField source="name" />
                    <IdField source="id" />
                </ArrayField> */}
                <ReferenceManyField
                    reference={'users/' + realmId + '/' + record.id + '/groups'}
                    target="userId"
                >
                    <Datagrid bulkActionButtons={false} sx={{ width: '100%' }}>
                        <TextField source="name" />
                        <IdField source="groupId" label="id" />
                    </Datagrid>
                </ReferenceManyField>
            </TabbedShowLayout.Tab>
            <TabbedShowLayout.Tab label={translate('page.user.roles.title')}>
                <SectionTitle
                    text={translate('page.user.roles.title')}
                    secondaryText={translate('page.user.roles.subTitle')}
                />
                <ArrayField source="roles">
                    <Datagrid bulkActionButtons={false}>
                        <TextField source="name" />
                        <IdField source="authority" />
                        <TextField source="provider" />
                    </Datagrid>
                </ArrayField>
                <SectionTitle
                    text={translate('page.user.roles.permissionTitle')}
                    secondaryText={translate(
                        'page.user.roles.permissionSubTitle'
                    )}
                />
                <TextField source="id" />
            </TabbedShowLayout.Tab>
            <TabbedShowLayout.Tab
                label={translate('page.user.attributes.title')}
            >
                <SectionTitle
                    text={translate(
                        'page.user.attributes.identityPrimaryTitle'
                    )}
                    secondaryText={translate(
                        'page.user.attributes.identitySubTitle'
                    )}
                />
                <SectionTitle
                    text={translate(
                        'page.user.attributes.additionalPrimaryTitle'
                    )}
                    secondaryText={translate(
                        'page.user.attributes.additionalsubTitle'
                    )}
                />
                <TextField source="id" />
            </TabbedShowLayout.Tab>
            <TabbedShowLayout.Tab label={translate('page.user.tos.title')}>
                <SectionTitle
                    text={translate('page.user.tos.primaryTitle')}
                    secondaryText={translate('page.user.tos.subTitle')}
                />
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

const ShowToolBarActions = () => {
    const refresh = useRefresh();
    const record = useRecordContext();

    if (!record) return null;

    return (
        <TopToolbar>
            <ActiveButton />
            <InspectButton />
            <AuthoritiesDialogButton onSuccess={() => refresh()} />
            <DeleteWithDialogButton />
            <RefreshingExportButton />
        </TopToolbar>
    );
};
