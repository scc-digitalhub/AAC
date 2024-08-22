import { Avatar, Box, Stack, Typography } from '@mui/material';
import {
    ArrayField,
    BooleanField,
    Button,
    Datagrid,
    DeleteWithConfirmButton,
    Edit,
    FunctionField,
    IconButtonWithTooltip,
    Labeled,
    ReferenceArrayField,
    ReferenceArrayInput,
    ReferenceManyField,
    SaveButton,
    ShowButton,
    TabbedForm,
    TextField,
    Toolbar,
    TopToolbar,
    useDataProvider,
    useDelete,
    useEditContext,
    useGetList,
    useNotify,
    useRecordContext,
    useRefresh,
    useResourceContext,
    useTranslate,
    useUpdate,
    WrapperField,
} from 'react-admin';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { JsonSchemaInput } from '@dslab/ra-jsonschema-input';
import { InspectButton } from '@dslab/ra-inspect-button';
import { PageTitle } from '../components/PageTitle';
import { Page } from '../components/Page';
import { ActiveButton } from './activeButton';
import { AuthoritiesDialogButton } from '../components/AuthoritiesDialog';
import { RefreshingExportButton } from '../components/RefreshingExportButton';
import { useRootSelector } from '@dslab/ra-root-selector';
import { AuditListView } from '../audit/AuditList';
import { IdField } from '../components/IdField';
import { ResourceTitle } from '../components/ResourceTitle';
import { SectionTitle } from '../components/sectionTitle';
import { DeveloperIcon, AdminIcon } from '../developers/DeveloperIcon';
import { ConnectedApps } from './ConnectedApps';
import { useWatch, ControllerRenderProps } from 'react-hook-form';
import ContentSave from '@mui/icons-material/Save';
import { getIdpIcon } from '../idps/utils';
import { IdpIcon } from '../idps/IdpIcon';
import { NameField } from '../components/NameField';
import { DataGridBlankHeader } from '../components/DataGridBlankHeader';
import { DropDownButton } from '../components/DropdownButton';
import { RowButtonGroup } from '../components/RowButtonGroup';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import StopIcon from '@mui/icons-material/Stop';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import EnabledIcon from '@mui/icons-material/CheckCircleOutlined';
import DisabledIcon from '@mui/icons-material/CancelOutlined';
import WarningIcon from '@mui/icons-material/WarningOutlined';
import SecurityIcon from '@mui/icons-material/Security';
import LockIcon from '@mui/icons-material/Lock';
import LockOpenIcon from '@mui/icons-material/LockOpen';
import GppBadIcon from '@mui/icons-material/GppBad';

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
                <IconButtonWithTooltip label={'account.active'} color="success">
                    <EnabledIcon fontSize="small" />
                </IconButtonWithTooltip>
            ) : (
                <IconButtonWithTooltip label={'account.inactive'} color="error">
                    <WarningIcon fontSize="small" />
                </IconButtonWithTooltip>
            )}
            {record.emailVerified === true ? (
                <IconButtonWithTooltip
                    label={'account.verified'}
                    color="success"
                >
                    <VerifiedUserIcon fontSize="small" />
                </IconButtonWithTooltip>
            ) : (
                <IconButtonWithTooltip
                    label={'account.unverified'}
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
    const { root: realmId } = useRootSelector();
    const dataProvider = useDataProvider();
    const translate = useTranslate();
    const notify = useNotify();

    if (!record) return null;

    return (
        <TabbedForm
            toolbar={false}
            syncWithLocation={false}
            validate={values => ({ error: 'not editable' })}
        >
            <TabbedForm.Tab label={'tab.overview'}>
                <Labeled>
                    <TextField source="username" />
                </Labeled>
                <Labeled>
                    <TextField source="email" />
                </Labeled>
                <Labeled>
                    <TextField source="subjectId" />
                </Labeled>
            </TabbedForm.Tab>
            <TabbedForm.Tab label={'tab.account'}>
                <SectionTitle
                    text={translate('page.user.account.title')}
                    secondaryText={translate('page.user.account.subtitle')}
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

                <UserReferencedInput reference="groups" source="groups" />
            </TabbedForm.Tab>
            <TabbedForm.Tab label={'tab.roles'}>
                <SectionTitle
                    text={translate('page.user.roles.title')}
                    secondaryText={translate('page.user.roles.subtitle')}
                />
                <UserReferencedInput reference="roles" source="roles" />
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
                            icon={false}
                        />
                        <IdField source="id" label="id" />
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
                            icon={false}
                        />
                        <IdField source="id" label="id" />
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
                    <BooleanField source="tos" label="field.tosAccepted" />
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

const TabToolbar = () => (
    <Toolbar>
        <SaveButton />
    </Toolbar>
);

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

const UserReferencedInput = (props: { reference: string; source: string }) => {
    const { reference, source } = props;
    const record = useRecordContext();
    const { root: realmId } = useRootSelector();
    const dataProvider = useDataProvider();
    const notify = useNotify();
    const refresh = useRefresh();

    //fetch related to resolve relations
    const { data } = useGetList(reference, {
        pagination: { page: 1, perPage: 100 },
        sort: { field: 'name', order: 'ASC' },
    });

    if (!record) return null;

    //inflate back flattened fields
    const field = useWatch({ name: source, defaultValue: [] });
    const handleSave = e => {
        e.stopPropagation();
        if (
            dataProvider &&
            record &&
            data !== undefined &&
            field !== undefined
        ) {
            const value = data.filter(g => field.includes(g.id));

            dataProvider
                .invoke({
                    path:
                        'users/' + realmId + '/' + record.id + '/' + reference,
                    body: JSON.stringify(value),
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

    return (
        <>
            <ReferenceArrayInput
                source={source}
                reference={reference}
                sort={{ field: 'name', order: 'ASC' }}
            />
            <Toolbar sx={{ width: '100%' }}>
                <Button
                    label="ra.action.save"
                    startIcon={<ContentSave />}
                    onClick={handleSave}
                    variant="contained"
                    size="medium"
                />
            </Toolbar>
        </>
    );
};

const UserAccountsForm = () => {
    const record = useRecordContext();
    const { root: realmId } = useRootSelector();

    if (!record) return null;

    const icon = record ? (
        getIdpIcon(record.authority, {
            color: 'info',
        })
    ) : (
        <IdpIcon />
    );

    return (
        <ReferenceManyField
            reference={'users/' + realmId + '/' + record.id + '/account'}
            sort={{ field: 'username', order: 'ASC' }}
            target="accounts"
            label="accounts"
        >
            {/* {record.apps && record.apps.map(app =>  (
            <TextField source="name" />
        ))} */}

            <Datagrid
                bulkActionButtons={false}
                sx={{ width: '100%' }}
                header={<DataGridBlankHeader />}
            >
                <NameField
                    text="name"
                    secondaryText="email"
                    tertiaryText="authority"
                    source="name"
                    icon={icon}
                />
                <IdField source="id" label="id" />
                <FunctionField
                    source="status"
                    render={r => (
                        <Stack spacing={1} direction={'row'}>
                            <BooleanField
                                source="locked"
                                valueLabelTrue="account.locked"
                                valueLabelFalse="account.unlocked"
                                TrueIcon={LockIcon}
                                color={r.locked ? 'error' : 'primary'}
                                FalseIcon={EnabledIcon}
                            />
                            <BooleanField
                                source="confirmed"
                                valueLabelTrue="account.confirmed"
                                valueLabelFalse="account.unconfirmed"
                                TrueIcon={VerifiedUserIcon}
                                FalseIcon={WarningIcon}
                                color={r.confirmed ? 'primary' : 'error'}
                            />
                        </Stack>
                    )}
                />
                <RowButtonGroup label="⋮">
                    <DropDownButton>
                        <InspectButton />
                        <ToggleStatusButton reference={record.id as string} />
                        <VerifyButton reference={record.id as string} />
                        <ToggleConfirmButton reference={record.id as string} />
                        <DeleteWithConfirmButton redirect={false} />
                    </DropDownButton>
                </RowButtonGroup>
            </Datagrid>
        </ReferenceManyField>
    );
};

export const ToggleStatusButton = (props: { reference: string }) => {
    const { reference } = props;
    const record = useRecordContext();
    const resource = useResourceContext();
    const dataProvider = useDataProvider();
    const { root: realmId } = useRootSelector();
    const notify = useNotify();
    const refresh = useRefresh();

    const handleDisable = () => {
        if (dataProvider && record) {
            dataProvider
                .invoke({
                    path:
                        'users/' +
                        realmId +
                        '/' +
                        reference +
                        '/account/' +
                        record.id +
                        '/lock',
                    body: JSON.stringify({}),
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

    const handleEnable = () => {
        if (dataProvider && record) {
            dataProvider
                .invoke({
                    path:
                        'users/' +
                        realmId +
                        '/' +
                        reference +
                        '/account/' +
                        record.id +
                        '/lock',
                    options: {
                        method: 'DELETE',
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
            {!record.locked ? (
                <Button
                    onClick={e => {
                        handleDisable();
                        e.stopPropagation();
                    }}
                    label="action.lock"
                    color="warning"
                    startIcon={<LockIcon />}
                ></Button>
            ) : (
                <Button
                    onClick={e => {
                        handleEnable();
                        e.stopPropagation();
                    }}
                    label="action.unlock"
                    color="success"
                    startIcon={<LockOpenIcon />}
                ></Button>
            )}
        </>
    );
};

const VerifyButton = (props: { reference: string }) => {
    const { reference } = props;
    const record = useRecordContext();
    const resource = useResourceContext();
    const dataProvider = useDataProvider();
    const { root: realmId } = useRootSelector();
    const notify = useNotify();
    const refresh = useRefresh();

    const handleClick = () => {
        if (dataProvider && record) {
            dataProvider
                .invoke({
                    path:
                        'users/' +
                        realmId +
                        '/' +
                        reference +
                        '/account/' +
                        record.id +
                        '/confirm',
                    body: JSON.stringify({}),
                    options: {
                        method: 'POST',
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

    if (!record || !record.email) return null;
    return (
        <Button
            onClick={e => {
                handleClick();
                e.stopPropagation();
            }}
            label="action.verify"
            color="success"
            startIcon={<SecurityIcon />}
        ></Button>
    );
};

const ToggleConfirmButton = (props: { reference: string }) => {
    const { reference } = props;
    const record = useRecordContext();
    const resource = useResourceContext();
    const dataProvider = useDataProvider();
    const { root: realmId } = useRootSelector();
    const notify = useNotify();
    const refresh = useRefresh();

    const handleClick = action => {
        if (dataProvider && record) {
            dataProvider
                .invoke({
                    path:
                        'users/' +
                        realmId +
                        '/' +
                        reference +
                        '/account/' +
                        record.id +
                        '/confirm',
                    body: JSON.stringify({}),
                    options: {
                        method: action,
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

    if (!record || !record.email) return null;
    return (
        <>
            {record.confirmed === false ? (
                <Button
                    onClick={e => {
                        handleClick('PUT');
                        e.stopPropagation();
                    }}
                    label="action.confirm"
                    color="warning"
                    startIcon={<VerifiedUserIcon />}
                ></Button>
            ) : (
                <Button
                    onClick={e => {
                        handleClick('DELETE');
                        e.stopPropagation();
                    }}
                    label="action.reset"
                    color="warning"
                    startIcon={<GppBadIcon />}
                ></Button>
            )}
        </>
    );
};

export const ToggleUserStatusButton = () => {
    const record = useRecordContext();
    const resource = useResourceContext();
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
