import { Divider, DividerProps } from '@mui/material';

export const Spacer = (props: SpacerProps) => {
    const { space, sx, ...rest } = props;
    const direction =
        rest?.orientation === 'vertical'
            ? 'borderRightWidth'
            : 'borderBottomWidth';
    return (
        <Divider
            sx={{
                borderColor: 'transparent',
                ...sx,
                ...{ [direction]: space ? space : '1em' },
            }}
            {...rest}
        />
    );
};

export interface SpacerProps extends DividerProps {
    space?: string;
}
