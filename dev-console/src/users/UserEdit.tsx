import { Avatar, Box, Typography } from '@mui/material';
import {
    ArrayField,
    AutocompleteArrayInput,
    BooleanField,
    Button,
    Datagrid,
    Edit,
    IconButtonWithTooltip,
    Labeled,
    ReferenceArrayInput,
    ReferenceManyField,
    TabbedForm,
    TextField,
    Toolbar,
    TopToolbar,
    useDataProvider,
    useGetList,
    useNotify,
    useRecordContext,
    useRefresh,
    useResourceContext,
    useTranslate,
} from 'react-admin';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { InspectButton } from '@dslab/ra-inspect-button';
import { Page } from '../components/Page';
import { AuthoritiesDialogButton } from '../components/AuthoritiesDialog';
import { RefreshingExportButton } from '../components/RefreshingExportButton';
import { useRootSelector } from '@dslab/ra-root-selector';
import { AuditListView } from '../audit/AuditList';
import { IdField } from '../components/IdField';
import { ResourceTitle } from '../components/ResourceTitle';
import { SectionTitle } from '../components/SectionTitle';
import { DeveloperIcon, AdminIcon } from '../developers/DeveloperIcon';
import { ConnectedApps } from './ConnectedApps';
import { useWatch } from 'react-hook-form';
import ContentSave from '@mui/icons-material/Save';
import { NameField } from '../components/NameField';
import { DataGridBlankHeader } from '../components/DataGridBlankHeader';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import EnabledIcon from '@mui/icons-material/CheckCircleOutlined';
import WarningIcon from '@mui/icons-material/WarningOutlined';
import GppBadIcon from '@mui/icons-material/GppBad';
import { UserAccountsForm } from './UserAccounts';
import LockIcon from '@mui/icons-material/Lock';
import LockOpenIcon from '@mui/icons-material/LockOpen';
import { ReferencedArrayInput } from '../components/ReferencedArrayInput';

export const UserEdit = () => {
    return (
        <Page>
            <Edit
                actions={<EditToolBarActions />}
                mutationMode="pessimistic"
                component={Box}
                redirect={'edit'}
                queryOptions={{ meta: { flatten: ['roles', 'groups'] } }}
            >
                <ResourceTitle text={<UserTitle />} icon={<UserAvatar />} />
                <UserForm />
            </Edit>
        </Page>
    );
};

const UserTitle = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <Typography variant="h4" sx={{ pt: 0, pb: 1, textAlign: 'left' }}>
            {record.username}{' '}
            {record.status === 'active' ? (
                <IconButtonWithTooltip label={'field.account.active'} color="success">
                    <EnabledIcon fontSize="small" />
                </IconButtonWithTooltip>
            ) : (
                <IconButtonWithTooltip label={'field.account.inactive'} color="error">
                    <WarningIcon fontSize="small" />
                </IconButtonWithTooltip>
            )}
            {record.emailVerified === true ? (
                <IconButtonWithTooltip
                    label={'field.account.verified'}
                    color="success"
                >
                    <VerifiedUserIcon fontSize="small" />
                </IconButtonWithTooltip>
            ) : (
                <IconButtonWithTooltip
                    label={'field.account.unverified'}
                    color="error"
                >
                    <GppBadIcon fontSize="small" />
                </IconButtonWithTooltip>
            )}
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

const UserForm = () => {
    const record = useRecordContext();
    const translate = useTranslate();

    if (!record) return null;

    return (
        <TabbedForm
            toolbar={false}
            syncWithLocation={false}
            validate={values => ({ error: 'not editable' })}
        >
            <TabbedForm.Tab label={'tab.overview'}>
                <Labeled>
                    <TextField source="username" label="field.username.name" />
                </Labeled>
                <Labeled>
                    <TextField source="email" label="field.email.name" />
                </Labeled>
                <Labeled>
                    <TextField
                        source="subjectId"
                        label="field.subjectId.name"
                    />
                </Labeled>
            </TabbedForm.Tab>
            <TabbedForm.Tab label={'tab.accounts'}>
                <SectionTitle
                    text={translate('page.user.accounts.title')}
                    secondaryText={translate('page.user.accounts.subtitle')}
                />
                <UserAccountsForm />
            </TabbedForm.Tab>
            <TabbedForm.Tab label={'tab.apps'}>
                <SectionTitle
                    text={translate('page.user.apps.title')}
                    secondaryText={translate('page.user.apps.subtitle')}
                />
                <ConnectedApps />
            </TabbedForm.Tab>
            <TabbedForm.Tab label={'tab.groups'}>
                <SectionTitle
                    text={translate('page.user.groups.title')}
                    secondaryText={translate('page.user.groups.subtitle')}
                />

                <ReferencedArrayInput
                    reference="groups"
                    source="groups"
                    label="field.groups.name"
                    helperText="field.groups.helperText"
                />
            </TabbedForm.Tab>
            <TabbedForm.Tab label={'tab.roles'}>
                <SectionTitle
                    text={translate('page.user.roles.title')}
                    secondaryText={translate('page.user.roles.subtitle')}
                />
                <ReferencedArrayInput
                    reference="roles"
                    source="roles"
                    label="field.roles.name"
                    helperText="field.roles.helperText"
                />
            </TabbedForm.Tab>
            <TabbedForm.Tab label={'tab.attributes'}>
                <SectionTitle
                    text={translate('page.user.attributes.identity.title')}
                    secondaryText={translate(
                        'page.user.attributes.identity.subtitle'
                    )}
                />

                <ArrayField source="identities">
                    <Datagrid
                        bulkActionButtons={false}
                        sx={{ width: '100%' }}
                        header={<DataGridBlankHeader />}
                    >
                        <NameField
                            text="username"
                            secondaryText="emailAddress"
                            tertiaryText="authority"
                            source="name"
                            label="field.name.name"
                            icon={false}
                        />
                        <IdField source="id" label="field.id.name" />
                        <InspectButton variant="contained" />
                    </Datagrid>
                </ArrayField>
                <br />
                <br />
                <SectionTitle
                    text={translate('page.user.attributes.attributes.title')}
                    secondaryText={translate(
                        'page.user.attributes.attributes.subtitle'
                    )}
                />
                <ArrayField source="attributes">
                    <Datagrid
                        bulkActionButtons={false}
                        sx={{ width: '100%' }}
                        header={<DataGridBlankHeader />}
                    >
                        <NameField
                            text="identifier"
                            tertiaryText="authority"
                            source="identifier"
                            label="field.identifier.name"
                            icon={false}
                        />
                        <IdField source="id" label="field.id.name" />
                        <InspectButton variant="contained" />
                    </Datagrid>
                </ArrayField>
            </TabbedForm.Tab>
            <TabbedForm.Tab label={'tab.tos'}>
                <SectionTitle
                    text={translate('page.user.tos.title')}
                    secondaryText={translate('page.user.tos.subtitle')}
                />
                <Labeled>
                    <BooleanField source="tos" label="field.tosAccepted.name" />
                </Labeled>
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.audit">
                <SectionTitle
                    text="page.apps.audit.title"
                    secondaryText="page.apps.audit.subTitle"
                />
                <ReferenceManyField reference="audit" target="principal">
                    <AuditListView />
                </ReferenceManyField>
            </TabbedForm.Tab>
        </TabbedForm>
    );
};

const EditToolBarActions = () => {
    const refresh = useRefresh();
    const record = useRecordContext();

    if (!record) return null;

    return (
        <TopToolbar>
            <ToggleUserStatusButton />
            <InspectButton />
            <AuthoritiesDialogButton onSuccess={() => refresh()} />
            <DeleteWithDialogButton />
            <RefreshingExportButton />
        </TopToolbar>
    );
};

export const ToggleUserStatusButton = () => {
    const record = useRecordContext();
    const dataProvider = useDataProvider();
    const { root: realmId } = useRootSelector();
    const notify = useNotify();
    const refresh = useRefresh();

    const handleStatus = status => {
        if (dataProvider && record) {
            dataProvider
                .invoke({
                    path: 'users/' + realmId + '/' + record.id + '/status',
                    body: JSON.stringify({ status }),
                    options: {
                        method: 'PUT',
                    },
                })
                .then(() => {
                    notify('ra.notification.updated', {
                        messageArgs: { smart_count: 1 },
                    });
                    refresh();
                });
        }
    };

    if (!record) return null;
    return (
        <>
            {record.status === 'active' ? (
                <Button
                    onClick={e => {
                        handleStatus('inactive');
                        e.stopPropagation();
                    }}
                    label="action.disable"
                    color="warning"
                    startIcon={<LockIcon />}
                ></Button>
            ) : (
                <Button
                    onClick={e => {
                        handleStatus('active');
                        e.stopPropagation();
                    }}
                    label="action.enable"
                    color="success"
                    startIcon={<LockOpenIcon />}
                ></Button>
            )}
        </>
    );
};
