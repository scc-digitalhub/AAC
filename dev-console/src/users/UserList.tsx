import {
    IconButton,
    Stack,
    Dialog,
    DialogContent,
    DialogTitle,
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
    SimpleShowLayout,
    useNotify,
    useDelete,
    useUpdate,
    useRefresh,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import CheckCircleOutlineOutlinedIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import VisibilityIcon from '@mui/icons-material/Visibility';
import { DeleteButtonDialog } from '../components/DeleteButtonDialog';
import React from 'react';
import AceEditor from 'react-ace';
import 'ace-builds/src-noconflict/mode-json';
import 'ace-builds/src-noconflict/theme-github';
import BlockIcon from '@mui/icons-material/Block';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import RemoveCircleIcon from '@mui/icons-material/RemoveCircle';

export const UserList = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };

    return (
        <List
            queryOptions={options}
            empty={<Empty />}
            actions={<UserListActions />}
            filters={UserFilters}
            sort={{ field: 'username', order: 'DESC' }}
        >
            <Datagrid>
                <WrapperField>
                    <Stack>
                        <TextField source="username" />
                        {`${params.realmId}` !== 'system' && (
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
                    filter={{ realm: params.realmId }}
                    source="authorities"
                    emptyText=""
                >
                    <Datagrid bulkActionButtons={false} empty={<></>}>
                        <ChipField source="role" size="small" />
                    </Datagrid>
                </ArrayField>
                <ShowUserButton />
                <InspectUserButton />
                <DeleteButtonDialog
                    confirmTitle="User Deletion"
                    mutationOptions={options}
                    redirect={`/users/r/${params.realmId}`}
                />
                <ActiveButton />
            </Datagrid>
        </List>
    );
};

const UserFilters = [<SearchInput source="q" alwaysOn />];

const UserListActions = () => {
    const params = useParams();
    const to = `/users/r/${params.realmId}/create`;
    return (
        <TopToolbar>
            <CreateButton
                variant="contained"
                label="Add/Invite User"
                sx={{ marginLeft: 2 }}
                to={to}
            />
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

const ShowUserButton = () => {
    const record = useRecordContext();
    const params = useParams();
    const redirect = useRedirect();
    const realmId = params.realmId;
    const to = '/users/r/' + realmId + '/' + record.id;
    const handleClick = () => {
        redirect(to);
    };
    if (!record) return null;
    return (
        <>
            <Button onClick={handleClick} label="Show"></Button>
        </>
    );
};

const InspectUserButton = () => {
    const record = useRecordContext();
    const [open, setOpen] = React.useState(false);
    if (!record) return null;
    let body = JSON.stringify(record, null, '\t');
    const handleClick = () => {
        setOpen(true);
    };
    const handleClose = () => {
        setOpen(false);
    };
    return (
        <>
            <>
                <Button onClick={handleClick} label="Inspect">
                    {<VisibilityIcon />}
                </Button>
            </>
            <Dialog
                open={open}
                onClose={handleClose}
                fullWidth
                maxWidth="md"
                sx={{
                    '.MuiDialog-paper': {
                        position: 'absolute',
                        top: 50,
                    },
                }}
            >
                <DialogTitle bgcolor={'#0066cc'} color={'white'}>
                    Inpsect Json
                </DialogTitle>
                <DialogContent>
                    <SimpleShowLayout>
                        <Typography>Raw JSON</Typography>
                        <Box>
                            <AceEditor
                                setOptions={{
                                    useWorker: false,
                                }}
                                mode="json"
                                value={body}
                                width="100%"
                                maxLines={20}
                                wrapEnabled
                                theme="github"
                                showPrintMargin={false}
                            ></AceEditor>
                        </Box>
                    </SimpleShowLayout>
                </DialogContent>
            </Dialog>
        </>
    );
};

const Empty = () => {
    const params = useParams();
    const to = `/users/r/${params.realmId}/create`;
    return (
        <Box textAlign="center" mt={30} ml={70}>
            <Typography variant="h6" paragraph>
                No user, create one
            </Typography>
            <CreateButton variant="contained" label="New User" to={to} />
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
