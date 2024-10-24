import {
    useTranslate,
    useGetIdentity,
    Labeled,
    TextField,
    DeleteWithConfirmButton,
    LinearProgress,
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

import { RealmsList } from '../resources/realms';

export const RealmsPage = () => {
    const translate = useTranslate();
    const { isLoading, data } = useGetIdentity();

    if (isLoading || !data) {
        return <LinearProgress />;
    }

    return (
        <Box component="div">
            <PageTitle
                text={translate('page.realms.header')}
                secondaryText={translate('page.realms.description')}
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

            <RealmsList />
        </Box>
    );
};
