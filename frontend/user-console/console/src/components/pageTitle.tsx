import { Typography, Paper, Container } from '@mui/material';
import { isValidElement, ReactElement } from 'react';

export const PageTitle = (props: PageTitleProps) => {
    const { text, secondaryText, icon } = props;

    return (
        <Paper elevation={0} sx={{ pt: 2, pb: 2, textAlign: 'center' }}>
            <Container maxWidth="md">
                {icon && isValidElement(icon) ? icon : ''}
                <Typography
                    variant="h4"
                    sx={{ pt: 0, pb: 1, textAlign: 'center' }}
                >
                    {text}
                </Typography>
                {secondaryText && (
                    <Typography
                        variant="h6"
                        sx={{ pt: 0, pb: 1, textAlign: 'center' }}
                    >
                        {secondaryText}
                    </Typography>
                )}
            </Container>
        </Paper>
    );
};

export interface PageTitleProps {
    text: string;
    secondaryText?: string;
    icon?: ReactElement;
}
