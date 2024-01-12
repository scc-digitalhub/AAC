import {
    useTranslate,
    useGetIdentity,
    Labeled,
    TextField,
    DeleteWithConfirmButton,
    LinearProgress,
    useLogout,
} from 'react-admin';
import {
    Box,
    Grid,
    Typography,
    Card,
    ListItem,
    Avatar,
    Alert,
} from '@mui/material';
import GroupIcon from '@mui/icons-material/Group';
import DeleteForeverIcon from '@mui/icons-material/DeleteForever';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import WarningIcon from '@mui/icons-material/Warning';

import { List as MuiList } from '@mui/material';

import { PageTitle } from '../components/pageTitle';
import { CardToolbar } from '../components/cardToolbar';
import CreateButton from '../components/createButton';

import { AccountsList } from '../resources/accounts';

const UserActions = ({ user }: { user: any }) => {
    const translate = useTranslate();
    const logout = useLogout();

    const account = user.identities.find(
        (i: any) => i.authority === 'internal'
    );

    return (
        // <RecordContextProvider record={data}>
        <Card sx={{ width: 1, p: 2 }}>
            {!account && (
                <Box sx={{ mb: 2 }}>
                    <Typography variant="h6" sx={{ fontWeight: 600, mb: 1 }}>
                        {translate('page.accounts.register_user.title')}
                    </Typography>
                    <Alert severity="info">
                        {translate('page.accounts.register_user.text')}
                    </Alert>
                    <CardToolbar variant="dense" sx={{ width: 1, mt: 1 }}>
                        <CreateButton
                            label="action.register"
                            icon={<PersonAddIcon />}
                            disabled
                        />
                    </CardToolbar>
                </Box>
            )}
            <Box>
                <Typography variant="h6" sx={{ fontWeight: 600 }}>
                    {translate('page.accounts.delete_user.title')}
                </Typography>
                <Typography sx={{ mb: 2 }}>
                    {translate('page.accounts.delete_user.text')}
                </Typography>
                <CardToolbar variant="dense" sx={{ width: 1 }}>
                    <DeleteWithConfirmButton
                        record={user}
                        resource={'details'}
                        label="page.accounts.delete_user.action"
                        confirmTitle="page.accounts.delete_user.confirm"
                        confirmContent="page.accounts.delete_user.content"
                        icon={<DeleteForeverIcon />}
                        translateOptions={{ id: user.username }}
                        onClick={e => logout()}
                    />
                </CardToolbar>
            </Box>
        </Card>
        // </RecordContextProvider>
    );
};

const UserProfile = ({ user }: { user: any }) => {
    const translate = useTranslate();
    const account = user.identities.find(
        (i: any) => i.authority === 'internal'
    );

    return (
        <Card sx={{ width: 1, p: 2 }}>
            {account && (
                <Typography variant="h6" sx={{ fontWeight: 600 }}>
                    {translate('page.accounts.registered_user')}
                </Typography>
            )}
            {!account && (
                <Typography
                    variant="h6"
                    color="warning.main"
                    sx={{ fontWeight: 600 }}
                >
                    <WarningIcon />{' '}
                    {translate('page.accounts.unregistered_user')}
                </Typography>
            )}

            <MuiList>
                <ListItem>
                    <Labeled>
                        <TextField
                            label="username"
                            source="username"
                            record={user}
                        />
                    </Labeled>
                </ListItem>

                <ListItem>
                    <Labeled>
                        <TextField label="id" source="id" record={user} />
                    </Labeled>
                </ListItem>
            </MuiList>
        </Card>
    );
};

export const AccountsPage = () => {
    const translate = useTranslate();
    const { isLoading, data } = useGetIdentity();

    if (isLoading || !data) {
        return <LinearProgress />;
    }

    return (
        <Box component="div">
            <PageTitle
                text={translate('page.accounts.header')}
                secondaryText={translate('page.accounts.description')}
                icon={
                    <Avatar
                        sx={{
                            width: 72,
                            height: 72,
                            mb: 2,
                            alignItems: 'center',
                            display: 'inline-block',
                            textTransform: 'uppercase',
                            lineHeight: '102px',
                            backgroundColor: '#0066cc',
                        }}
                    >
                        <GroupIcon sx={{ fontSize: 48 }} />
                    </Avatar>
                }
            />
            <Grid container spacing={2}>
                <Grid item md={6} xs={12} key="accounts.userprofile">
                    <UserProfile user={data} />
                </Grid>
                <Grid item md={6} xs={12} key="accounts.useractions">
                    <UserActions user={data} />
                </Grid>
            </Grid>
            <Typography variant="h5" sx={{ mt: 8, mb: 3 }}>
                {translate('page.accounts.header')}
            </Typography>
            <AccountsList />
        </Box>
    );
};
