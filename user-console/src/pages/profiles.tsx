import { useTranslate } from 'react-admin';
import { Box, Avatar } from '@mui/material';
import PersonIcon from '@mui/icons-material/Person';

import { PageTitle } from '../components/pageTitle';
import { ProfilesList } from '../resources/profiles';

export const ProfilesPage = () => {
    const translate = useTranslate();
    return (
        <Box component="div">
            <PageTitle
                text={translate('page.profiles.header')}
                secondaryText={translate('page.profiles.description')}
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
                        <PersonIcon sx={{ fontSize: 48 }} />
                    </Avatar>
                }
            />
            <ProfilesList />
        </Box>
    );
};
