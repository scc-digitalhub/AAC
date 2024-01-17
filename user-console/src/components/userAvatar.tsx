import { Avatar } from '@mui/material';
import { useGetIdentity } from 'react-admin';

const defaultStyle = {
    width: 32,
    height: 32,
    mb: 2,
    alignItems: 'center',
    display: 'inline-block',
    textTransform: 'uppercase',
    fontSize: 16,
    lineHeight: '32px',
    backgroundColor: '#0066cc',
    textAlign: 'center',
};

export const UserAvatar = (params: UserAvatarParams) => {
    const { user: userFromParams, size = 'small' } = params;
    const { data: userFromHook, isLoading } = useGetIdentity();

    const user = userFromParams || userFromHook;

    return (
        <>
            {user.username && (
                <Avatar sx={getStyle(size)}>
                    {user.username.substring(0, 2)}
                </Avatar>
            )}
        </>
    );
};

function getStyle(size: string) {
    if (size === 'large') {
        return Object.assign({}, defaultStyle, {
            width: 72,
            height: 72,
            fontSize: 36,
            lineHeight: '64px',
        });
    }
    if (size === 'medium') {
        return Object.assign({}, defaultStyle, {
            width: 48,
            height: 48,
            fontSize: 24,
            lineHeight: '48px',
        });
    }

    return defaultStyle;
}

export type UserAvatarParams = {
    size?: 'small' | 'medium' | 'large';
    user?: any;
};
