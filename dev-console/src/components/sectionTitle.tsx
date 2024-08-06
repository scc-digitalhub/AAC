import { Box, Typography } from '@mui/material';
import { ReactElement } from 'react';

export const SectionTitle = (props: SectionTitleProps) => {
    const { text, secondaryText, icon } = props;

    return (
        <Box marginBottom={2}>
            <Typography variant="h5" sx={{ pt: 0, pb: 1, textAlign: 'left' }}>
                {text}
            </Typography>
            {secondaryText && (
                <Typography
                    variant="h6"
                    sx={{ pt: 0, pb: 1, textAlign: 'left' }}
                >
                    {secondaryText}
                </Typography>
            )}
        </Box>
    );
};

export interface SectionTitleProps {
    text: string;
    secondaryText?: string | number | undefined;
    icon?: ReactElement;
}
