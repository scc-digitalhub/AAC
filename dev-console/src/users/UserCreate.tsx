import { CreateBase, Form, SaveButton, TextInput, Toolbar } from 'react-admin';
import {
    Card,
    CardContent,
    Box,
    Divider,
    Typography,
    DialogActions,
    Stack,
} from '@mui/material';
import { Page } from '../components/Page';

export const UserCreate = () => {
    const transform = (data: any) => {
        return {
            ...data,
        };
    };

    return (
        <CreateBase transform={transform}>
            <UserInviteForm />
        </CreateBase>
    );
};
export const UserInviteForm = () => {
    return (
        <Form>
            <Page>
                <Stack rowGap={2}>
                    <TextInput
                        source="email"
                        label="field.email.name"
                        helperText="field.email.helperText"
                        fullWidth
                        required
                    />
                </Stack>
            </Page>
            <DialogActions>
                <SaveButton label="action.invite" variant="text" />
            </DialogActions>
        </Form>
    );
};
