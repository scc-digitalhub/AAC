import { IconButton, Stack, Box, Typography } from '@mui/material';
import {
    List,
    Datagrid,
    TextField,
    TopToolbar,
    ArrayField,
    ChipField,
    WrapperField,
    useRecordContext,
    SearchInput,
    Button,
    useNotify,
    useUpdate,
    useRefresh,
    ShowButton,
    EditButton,
} from 'react-admin';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import CheckCircleOutlineOutlinedIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import BlockIcon from '@mui/icons-material/Block';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import { InspectButton } from '@dslab/ra-inspect-button';
import { useRootSelector } from '@dslab/ra-root-selector';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { CreateInDialogButton } from '@dslab/ra-dialog-crud';
import { UserCreateForm } from './UserCreate';
import { DropDownButton } from '../components/DropdownButton';
import { RowButtonGroup } from '../components/RowButtonGroup';

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
                    {record?.role && <ChipField source="role" size="small" />}
                </ArrayField>
                <RowButtonGroup label="⋮">
                    <DropDownButton>
                        <ShowButton />
                        <EditButton />
                        <InspectButton />
                        <ActiveButton />
                        <DeleteWithDialogButton />
                    </DropDownButton>
                </RowButtonGroup>
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
    const notify = useNotify();
    const refresh = useRefresh();
    const [inactive] = useUpdate(
        'users',
        {
            id: record.id + '/status',
            data: record,
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
        },
        {
            onSuccess: () => {
                notify(`user ` + record.id + ` enabled successfully`);
                refresh();
            },
        }
    );

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
        </>
    );
};
