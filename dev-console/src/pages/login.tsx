import { useTranslate } from 'react-admin';
import { Container, Avatar } from '@mui/material';

import { PageTitle } from '../components/PageTitle';
import LoginIcon from '@mui/icons-material/Login';

export const LoginPage = () => {
    const translate = useTranslate();

    return (
        <Container maxWidth={false}>
            <PageTitle
                text={translate('page.login.header')}
                secondaryText={translate('page.login.description')}
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
                        <LoginIcon sx={{ fontSize: 48 }} />
                    </Avatar>
                }
            />
        </Container>
    );
};
