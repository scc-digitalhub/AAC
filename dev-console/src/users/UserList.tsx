import { IconButton, Stack } from '@mui/material';
import {
    List,
    Datagrid,
    TextField,
    TopToolbar,
    CreateButton,
    Empty,
    ArrayField,
    ChipField,
    WrapperField,
    useRecordContext,
    SearchInput,
    useRedirect,
    Button,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import CheckCircleOutlineOutlinedIcon from '@mui/icons-material/CheckCircleOutlineOutlined';

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