import { Typography } from '@mui/material';
import { ReactElement } from 'react';

export const TabTitle = (props: TabTitleProps) => {
    const { text, secondaryText, icon } = props;

    return (
            <>
                <Typography
                    variant="h5"
                    sx={{ pt: 0, pb: 1, textAlign: 'left' }}
                >
                    {text}
                </Typography>
                {secondaryText && (
                    <Typography
                    variant="h6"

                        sx={{ pt: 0, pb: 1, textAlign: 'left' }}
                    >
                        {secondaryText}
                    </Typography>
                )}</>
    );
};

export interface TabTitleProps {
    text: string;
    secondaryText?: string|number|undefined;
    icon?: ReactElement;
}
