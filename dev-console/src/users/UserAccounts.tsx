import { Stack } from '@mui/material';
import {
    BooleanField,
    Button,
    Datagrid,
    DeleteWithConfirmButton,
    FunctionField,
    ReferenceManyField,
    useDataProvider,
    useNotify,
    useRecordContext,
    useRefresh,
    useResourceContext,
} from 'react-admin';
import { InspectButton } from '@dslab/ra-inspect-button';
import { useRootSelector } from '@dslab/ra-root-selector';
import { IdField } from '../components/IdField';
import { getIdpIcon } from '../idps/utils';
import { IdpIcon } from '../idps/IdpIcon';
import { NameField } from '../components/NameField';
import { DataGridBlankHeader } from '../components/DataGridBlankHeader';
import { DropDownButton } from '../components/DropdownButton';
import { RowButtonGroup } from '../components/RowButtonGroup';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import EnabledIcon from '@mui/icons-material/CheckCircleOutlined';
import WarningIcon from '@mui/icons-material/WarningOutlined';
import SecurityIcon from '@mui/icons-material/Security';
import LockIcon from '@mui/icons-material/Lock';
import LockOpenIcon from '@mui/icons-material/LockOpen';
import GppBadIcon from '@mui/icons-material/GppBad';

export const UserAccountsForm = () => {
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
            label="field.accounts.name"
        >
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
                    label="field.name.name"
                    icon={icon}
                />
                <IdField source="id" label="field.id.name" />
                <FunctionField
                    source="status"
                    label="field.status.name"
                    render={r => (
                        <Stack spacing={1} direction={'row'}>
                            <BooleanField
                                source="locked"
                                valueLabelTrue="field.account.locked"
                                valueLabelFalse="field.account.unlocked"
                                TrueIcon={LockIcon}
                                color={r.locked ? 'error' : 'primary'}
                                FalseIcon={EnabledIcon}
                            />
                            <BooleanField
                                source="confirmed"
                                valueLabelTrue="field.account.confirmed"
                                valueLabelFalse="field.account.unconfirmed"
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

export const VerifyButton = (props: { reference: string }) => {
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

export const ToggleConfirmButton = (props: { reference: string }) => {
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
