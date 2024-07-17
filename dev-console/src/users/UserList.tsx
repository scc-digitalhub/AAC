import {
    IconButton,
    Stack,
    Box,
    Typography,
} from '@mui/material';
import {
    List,
    Datagrid,
    TextField,
    TopToolbar,
    CreateButton,
    ArrayField,
    ChipField,
    WrapperField,
    useRecordContext,
    SearchInput,
    useRedirect,
    Button,
    useNotify,
    useUpdate,
    useRefresh,
    ShowButton,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import CheckCircleOutlineOutlinedIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import React from 'react';
import BlockIcon from '@mui/icons-material/Block';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import { InspectButton } from '@dslab/ra-inspect-button';
import { useRootSelector } from '@dslab/ra-root-selector';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { CreateInDialogButton } from '@dslab/ra-dialog-crud';
import { UserCreateForm } from './UserCreate';

export const UserList = () => {
    const { root: realmId } = useRootSelector();
    const record = useRecordContext();
    return (
        <List
            empty={<Empty />}
            actions={<UserListActions />}
            filters={UserFilters}
            sort={{ field: 'username', order: 'DESC' }}
        >
            <Datagrid>
                <WrapperField>
                    <Stack>
                        <TextField source="username" />
                        {`${realmId}` !== 'system' && (
                            <span>
                                <TextField source="email" />
                                &nbsp;
                                <EmailVerified source="emailVerified" />
                            </span>
                        )}
                    </Stack>
                </WrapperField>
                <IdField source="id" />
                <ArrayField
                    filter={{ realm: realmId }}
                    source="authorities"
                    emptyText=""
                >
                    {record?.role && 
                        <ChipField source="role" size="small" />
                    }
                </ArrayField>
                <ShowButton />
                <InspectButton />
                <DeleteWithDialogButton confirmTitle="User Deletion"/>
                <ActiveButton />
            </Datagrid>
        </List>
    );
};

const UserFilters = [<SearchInput source="q" alwaysOn />];

const UserListActions = () => {
    return (
        <TopToolbar>
            <CreateInDialogButton
                    fullWidth
                    maxWidth={'md'}
                    variant="contained"
                    transform={transform}
                >
                    <UserCreateForm />
                </CreateInDialogButton>
        </TopToolbar>
    );
};

const IdField = (props: any) => {
    let s = props.source;
    const record = useRecordContext();
    if (!record) return null;
    return (
        <span>
            {record[s]}
            <IconButton
                onClick={() => {
                    navigator.clipboard.writeText(record[s]);
                }}
            >
                <ContentCopyIcon />
            </IconButton>
        </span>
    );
};

const EmailVerified = (props: any) => {
    let s = props.source;
    const record = useRecordContext();
    if (!record) return null;
    return (
        <span>
            {record[s] && (
                <IconButton title="Email Verified">
                    <CheckCircleOutlineOutlinedIcon
                        color="success"
                        fontSize="small"
                        sx={{ 'vertical-align': 'bottom' }}
                    />
                </IconButton>
            )}
        </span>
    );
};

const transform = (data: any) => {
    return {
        ...data,
    };
};
const Empty = () => {
    return (
        <Box textAlign="center" mt={30} ml={70}>
            <Typography variant="h6" paragraph>
                No user, create one
            </Typography>
            <CreateInDialogButton
                    fullWidth
                    maxWidth={'md'}
                    variant="contained"
                    transform={transform}
                >
                    <UserCreateForm />
                </CreateInDialogButton>
        </Box>
    );
};

const ActiveButton = () => {
    const record = useRecordContext();
    const params = useParams();
    const notify = useNotify();
    const refresh = useRefresh();
    const realmId = params.realmId;
    const [inactive] = useUpdate(
        'users',
        {
            id: record.id + '/status',
            data: record,
            meta: { realmId: realmId },
        },
        {
            onSuccess: () => {
                notify(`user ` + record.id + ` disabled successfully`);
                refresh();
            },
        }
    );
    const [active] = useUpdate(
        'users',
        {
            id: record.id + '/status',
            data: record,
            meta: { realmId: realmId },
        },
        {
            onSuccess: () => {
                notify(`user ` + record.id + ` enabled successfully`);
                refresh();
            },
        }
    );

    // const [block] = useUpdate(
    //     'users',
    //     {
    //         id: record.id + '/status',
    //         data: record,
    //         meta: { realmId: realmId },
    //     },
    //     {
    //         onSuccess: () => {
    //             notify(`user ` + record.id + ` blocked successfully`);
    //             refresh();
    //         },
    //     }
    // );

    if (!record) return null;
    return (
        <>
            {record.status !== 'inactive' && (
                <Button
                    onClick={() => {
                        record.status = 'inactive';
                        inactive();
                    }}
                    label="Deactivate"
                    startIcon={<BlockIcon />}
                ></Button>
            )}
            {record.status === 'inactive' && (
                <Button
                    onClick={() => {
                        record.status = 'active';
                        active();
                    }}
                    label="Activate"
                    startIcon={<PlayArrowIcon />}
                ></Button>
            )}
            {/* &nbsp;
            {record.status === 'active' && (
                <Button
                    onClick={() => {
                        record.status = 'blocked';
                        block();
                    }}
                    label="Block"
                    startIcon={<RemoveCircleIcon />}
                ></Button>
            )} */}
        </>
    );
};
